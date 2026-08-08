/**
 * Cloud Functions for Humble Contacts — account-deletion email verification.
 *
 * Flow (Google sign-in accounts):
 *   1. App calls the callable `requestAccountDeletion`. It stores a one-time token at
 *      `account_deletions/{uid}` and emails a confirmation link to the account email.
 *   2. The user taps the link, which opens the HTTPS `confirmAccountDeletion` endpoint
 *      (a plain cloudfunctions.net URL — no App Links domain required). It validates the
 *      token and flips `confirmed: true`.
 *   3. The app listens to `account_deletions/{uid}`; when `confirmed` becomes true it
 *      re-authenticates (silent Google) and deletes the account + data.
 *
 * SETUP REQUIRED before this works:
 *   - Set SMTP credentials for nodemailer as environment variables / secrets:
 *       SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM
 *     (e.g. SendGrid: SMTP_HOST=smtp.sendgrid.net, SMTP_PORT=587,
 *      SMTP_USER=apikey, SMTP_PASS=<sendgrid_api_key>, SMTP_FROM="Humble Contacts <no-reply@yourdomain>")
 *   - Deploy: `firebase deploy --only functions`
 *   - Firestore rule so the signed-in user can read/delete their own doc (see README/setup doc).
 */

const functions = require("firebase-functions/v1");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const crypto = require("crypto");
const nodemailer = require("nodemailer");

// Gemini API key — held ONLY in the Functions secret store, never in the app.
// Set with: firebase functions:secrets:set GEMINI_API_KEY
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

admin.initializeApp();
const db = admin.firestore();

const REGION = "us-central1";

async function buildTransport() {
  // Real delivery: use configured SMTP credentials.
  if (process.env.SMTP_HOST) {
    return nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: Number(process.env.SMTP_PORT || 587),
      secure: Number(process.env.SMTP_PORT) === 465,
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS,
      },
    });
  }

  // DEV/TEST fallback (no SMTP_* configured): nodemailer generates a throwaway
  // Ethereal account. The message is NOT delivered to a real inbox — a preview URL
  // is logged instead (see getTestMessageUrl below). Set SMTP_* for real delivery.
  const testAccount = await nodemailer.createTestAccount();
  functions.logger.warn(
    "No SMTP_* configured — using Ethereal test account; email is NOT delivered, see preview URL."
  );
  return nodemailer.createTransport({
    host: "smtp.ethereal.email",
    port: 587,
    secure: false,
    auth: { user: testAccount.user, pass: testAccount.pass },
  });
}

function htmlPage(title, message) {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title}</title>
<style>
  body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#f6f7f9;
       display:flex;min-height:100vh;margin:0;align-items:center;justify-content:center}
  .card{background:#fff;max-width:420px;padding:32px;border-radius:16px;
        box-shadow:0 6px 24px rgba(0,0,0,.08);text-align:center}
  h1{font-size:20px;margin:0 0 12px}
  p{color:#555;line-height:1.5;margin:0}
</style></head>
<body><div class="card"><h1>${title}</h1><p>${message}</p></div></body></html>`;
}

/**
 * Callable: create a one-time deletion token and email the confirmation link.
 */
exports.requestAccountDeletion = functions.https.onCall(async (data, context) => {
  const uid = context.auth && context.auth.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "You must be signed in.");
  }

  const email =
    (context.auth.token && context.auth.token.email) || (data && data.email);
  if (!email) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "No email address is associated with this account."
    );
  }

  const token = crypto.randomBytes(32).toString("hex");
  const ref = db.collection("account_deletions").doc(uid);
  await ref.set({
    token,
    confirmed: false,
    email,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  const project = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT;
  const confirmUrl =
    `https://${REGION}-${project}.cloudfunctions.net/confirmAccountDeletion` +
    `?uid=${encodeURIComponent(uid)}&token=${token}`;

  // Send the confirmation email. If this fails (e.g. bad SMTP credentials), roll back
  // the pending token so we don't leave a stale, unconfirmable request behind, and
  // surface a clear error to the app instead of a bare "internal" crash.
  let info;
  try {
    const transport = await buildTransport();
    info = await transport.sendMail({
      from: process.env.SMTP_FROM || process.env.SMTP_USER || "no-reply@humblecontacts.app",
      to: email,
      subject: "Confirm your Humble Contacts account deletion",
      text:
        "You requested to permanently delete your Humble Contacts account.\n\n" +
        "Confirm by opening this link:\n" +
        confirmUrl +
        "\n\nIf you didn't request this, ignore this email — your account stays safe.",
      html:
        `<p>You requested to permanently delete your <b>Humble Contacts</b> account.</p>` +
        `<p><a href="${confirmUrl}">Confirm account deletion</a></p>` +
        `<p style="color:#888;font-size:13px">If you didn't request this, ignore this email — your account stays safe.</p>`,
    });
  } catch (err) {
    functions.logger.error("Failed to send account-deletion confirmation email", err);
    await ref.delete().catch(() => {});
    throw new functions.https.HttpsError(
      "internal",
      "We couldn't send the confirmation email. Please try again later."
    );
  }

  // When using the Ethereal test fallback, log the preview URL so the email can be viewed.
  const previewUrl = nodemailer.getTestMessageUrl(info);
  if (previewUrl) {
    functions.logger.log("EMAIL_PREVIEW_URL", previewUrl);
  }

  return { ok: true };
});

/**
 * HTTPS: validate the token, then delete the account server-side with the Admin SDK.
 * Opened from the email link. Because this runs with admin privileges, NO client-side
 * re-authentication is needed — the app just drops to the logged-out state once it sees
 * `confirmed: true`.
 */
exports.confirmAccountDeletion = functions.https.onRequest(async (req, res) => {
  const uid = String(req.query.uid || "");
  const token = String(req.query.token || "");

  if (!uid || !token) {
    res.status(400).send(htmlPage("Invalid link", "This confirmation link is missing information."));
    return;
  }

  const ref = db.collection("account_deletions").doc(uid);
  const snap = await ref.get();

  if (!snap.exists || snap.data().token !== token) {
    res.status(400).send(
      htmlPage("Link expired", "This confirmation link is invalid or has already been used.")
    );
    return;
  }

  // Signal the app to drop to the logged-out state (it's watching this doc). Set BEFORE
  // deleting the Auth user so the client receives it while its token is still valid.
  await ref.update({
    confirmed: true,
    confirmedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // Server-side cascade delete with admin privileges (no re-auth required).
  try {
    // Firestore user doc.
    await db.collection("users").doc(uid).delete();

    // Firestore contacts owned by the user (chunked to respect batch limits).
    const contacts = await db.collection("contacts").where("ownerId", "==", uid).get();
    let batch = db.batch();
    let count = 0;
    for (const doc of contacts.docs) {
      batch.delete(doc.ref);
      count++;
      if (count % 400 === 0) {
        await batch.commit();
        batch = db.batch();
      }
    }
    await batch.commit();

    // Storage business-card images (best-effort).
    try {
      await admin.storage().bucket().deleteFiles({ prefix: `business_cards/${uid}/` });
    } catch (storageErr) {
      functions.logger.warn("Storage cleanup failed", storageErr);
    }

    // The Auth account itself.
    await admin.auth().deleteUser(uid);

    functions.logger.log("Account fully deleted (server-side)", uid);
  } catch (err) {
    functions.logger.error("Server-side deletion failed", err);
    res.status(500).send(
      htmlPage("Something went wrong", "We couldn't finish deleting your account. Please try again.")
    );
    return;
  }

  res.status(200).send(
    htmlPage("Account deleted", "Your account has been permanently deleted. You can close this page.")
  );
});

// ─────────────────────────────────────────────────────────────────────────────
// Business-card parsing (Gemini) — server-side so the API key never ships in the
// APK. The app posts only the OCR text; this function calls Gemini with the
// secret-held key and returns parsed contact JSON. (v2 callable.)
// ─────────────────────────────────────────────────────────────────────────────

const GEMINI_MODEL = "gemini-2.0-flash";

// Same extraction contract the app used before the key moved server-side.
const BUSINESS_CARD_PROMPT = (text) => `You are an expert at extracting information from business cards.

Extract these fields from the OCR text.

Return ONLY valid JSON.

Do not include markdown.
Do not include explanations.
Do not wrap in \`\`\`.

Rules:
- "name" = person's full name only.
- "designation" = job title.
- "company" = organization/company name.
- "email" = email address.
- "phone" = mobile phone number including country code if present.
- "linkedin" = LinkedIn profile URL or username.
- "address" = full postal address exactly as printed on the business card. Include street, building, city, state, postal code and country if present.
- If multiple phone numbers exist, choose the mobile number.
- Ignore websites, QR codes, slogans and all social media except LinkedIn.
- Never use a website URL as the address.
- If a field is unavailable, return an empty string.

JSON format:

{
  "name": "",
  "designation": "",
  "company": "",
  "email": "",
  "phone": "",
  "linkedin": "",
  "address": ""
}

OCR TEXT:

${text}`;

const EMPTY_CONTACT = {
  name: "",
  designation: "",
  company: "",
  email: "",
  phone: "",
  linkedin: "",
  address: "",
};

/**
 * Strip markdown fences / stray prose and pull the JSON object out of Gemini's
 * reply, then coerce it into the fixed ContactInfo shape. Never throws on
 * malformed model output — returns empty strings so the client can't crash.
 */
function toContactInfo(rawText) {
  if (!rawText) return { ...EMPTY_CONTACT };

  let cleaned = String(rawText)
    .replace(/```json/gi, "")
    .replace(/```/g, "")
    .trim();

  // If the model wrapped the JSON in prose, keep only the outermost { ... }.
  const first = cleaned.indexOf("{");
  const last = cleaned.lastIndexOf("}");
  if (first !== -1 && last !== -1 && last > first) {
    cleaned = cleaned.slice(first, last + 1);
  }

  let parsed;
  try {
    parsed = JSON.parse(cleaned);
  } catch (err) {
    functions.logger.warn("Gemini returned unparseable JSON; returning empty contact.");
    return { ...EMPTY_CONTACT };
  }

  const asString = (v) => (typeof v === "string" ? v : v == null ? "" : String(v));
  return {
    name: asString(parsed.name),
    designation: asString(parsed.designation),
    company: asString(parsed.company),
    email: asString(parsed.email),
    phone: asString(parsed.phone),
    linkedin: asString(parsed.linkedin),
    address: asString(parsed.address),
  };
}

exports.parseBusinessCard = onCall(
  { secrets: [GEMINI_API_KEY], region: REGION },
  async (request) => {
    // Require a signed-in caller — no anonymous access to the paid Gemini API.
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "You must be signed in.");
    }

    const text = request.data && typeof request.data.text === "string"
      ? request.data.text.trim()
      : "";
    if (!text) {
      throw new HttpsError("invalid-argument", "No OCR text was provided.");
    }

    const url =
      `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent` +
      `?key=${encodeURIComponent(GEMINI_API_KEY.value())}`;

    let res;
    try {
      res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ parts: [{ text: BUSINESS_CARD_PROMPT(text) }] }],
        }),
      });
    } catch (err) {
      functions.logger.error("Gemini request failed", err);
      throw new HttpsError("unavailable", "The parsing service is temporarily unavailable.");
    }

    if (!res.ok) {
      const detail = await res.text().catch(() => "");
      functions.logger.error(`Gemini HTTP ${res.status}`, detail);
      // Surface rate-limit distinctly; everything else is a generic upstream error.
      if (res.status === 429) {
        throw new HttpsError("resource-exhausted", "Too many requests. Please wait a few moments.");
      }
      throw new HttpsError("internal", "The parsing service returned an error.");
    }

    const body = await res.json().catch(() => null);
    const modelText =
      body &&
      body.candidates &&
      body.candidates[0] &&
      body.candidates[0].content &&
      body.candidates[0].content.parts &&
      body.candidates[0].content.parts[0] &&
      body.candidates[0].content.parts[0].text;

    // Malformed/fence-wrapped output is normalised here, never on the client.
    return toContactInfo(modelText);
  }
);
