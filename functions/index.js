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
const admin = require("firebase-admin");
const crypto = require("crypto");
const nodemailer = require("nodemailer");

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
  await db.collection("account_deletions").doc(uid).set({
    token,
    confirmed: false,
    email,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  const project = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT;
  const confirmUrl =
    `https://${REGION}-${project}.cloudfunctions.net/confirmAccountDeletion` +
    `?uid=${encodeURIComponent(uid)}&token=${token}`;

  const transport = await buildTransport();
  const info = await transport.sendMail({
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

  // When using the Ethereal test fallback, log the preview URL so the email can be viewed.
  const previewUrl = nodemailer.getTestMessageUrl(info);
  if (previewUrl) {
    functions.logger.log("EMAIL_PREVIEW_URL", previewUrl);
  }

  return { ok: true };
});

/**
 * HTTPS: validate the token and mark the deletion confirmed. Opened from the email link.
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

  await ref.update({
    confirmed: true,
    confirmedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  res.status(200).send(
    htmlPage(
      "Deletion confirmed",
      "Your account deletion is confirmed. Return to the Humble Contacts app to finish."
    )
  );
});
