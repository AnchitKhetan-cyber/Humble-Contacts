# Firestore security rules

The rules live in `firestore.rules` (repo root) and are referenced from `firebase.json`
(`firestore.rules` + `firestore.indexes.json`), so they deploy with the project.

## What the rules enforce
A request is allowed only when it is **authenticated** and concerns the **caller's own data**:

| Collection | Owner is identified by | Allowed |
|---|---|---|
| `contacts/{id}` | the `ownerId` **field** on the doc | read/update/delete if `resource.data.ownerId == request.auth.uid`; create if the new doc's `ownerId == request.auth.uid` |
| `users/{uid}` | the **document id** | read/write if `request.auth.uid == uid` |
| `account_deletions/{uid}` | the **document id** | read/delete if `request.auth.uid == uid`; content is written by the Cloud Function (Admin SDK bypasses rules), so client create/update is denied |
| everything else | — | denied (catch-all `allow read, write: if false`) |

## ⚠️ Step 1 — VERIFY the deployed rules first (gating; AC #1)

Do this **before** anything else and report the verdict to the manager. This needs Firebase
console access and two test accounts (User A and User B).

1. **Inspect:** Firebase console → Firestore Database → **Rules**. If you see anything like
   `allow read, write: if request.auth != null;` on `contacts` (no `ownerId` check) or
   `if true`, the rules are **OPEN**.
2. **Reproduce the breach:**
   - As **User B**, create a contact and note its document id (console → Data → `contacts`).
   - Sign in to the app (or a REST/console session) as **User A** and attempt to read
     `contacts/{that id}`.
   - **If A gets B's data → rules are OPEN → escalate to the manager immediately.**
   - Quick REST check (A's ID token):
     ```bash
     curl "https://firestore.googleapis.com/v1/projects/humble-contacts-2e9c9/databases/(default)/documents/contacts/<B_DOC_ID>" \
       -H "Authorization: Bearer <A_ID_TOKEN>"
     ```
     `200` + document = OPEN; `403 PERMISSION_DENIED` = closed.

Record: **open or closed**, the doc id used, and the timestamp.

## Step 2 — Deploy the hardened rules
```bash
firebase deploy --only firestore:rules
```

## Step 3 — Re-run the breach repro (AC #5)
Repeat the Step 1 reproduction. It must now return **`403 PERMISSION_DENIED`** for User A
reading User B's contact.

## Rules tests (emulator)
Automated tests live in `firestore-tests/` and cover owner-allowed / non-owner-denied /
unauthenticated-denied for reads and writes.

```bash
cd firestore-tests
npm install
npm test        # firebase emulators:exec --only firestore "node rules.test.js"
```

Requires the Firebase CLI and **JDK 21+** on PATH (the Firestore emulator refuses older Java).
Android Studio ships a JDK 21 at `…/Android Studio/jbr`; put its `bin` on PATH if your default
`java` is older.
