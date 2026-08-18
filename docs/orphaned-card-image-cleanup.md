# Business-card image cleanup on contact delete

Business-card photos are stored at `business_cards/{ownerId}/{contactId}.jpg`
(`ContactRepository.uploadBusinessCard`). Deleting a contact must also remove its image so
photos of third parties' cards don't linger in Storage (privacy + cost). Ticket #15.

## Two layers

1. **Client, best-effort** (`ContactRepository.kt`)
   - `deleteContact(contactId)` — after deleting the Firestore doc, deletes
     `business_cards/{uid}/{contactId}.jpg`. Wrapped in try/catch: a Storage failure is logged
     and swallowed, never blocking the doc delete.
   - `deleteAllContacts(ownerUid)` — after the batch delete, calls the existing
     `deleteBusinessCardImages(ownerUid)` (whole-folder clear), also best-effort.

2. **Server backstop** (`functions/index.js` → `cleanupContactCardImage`)
   - A v2 `onDocumentDeleted("contacts/{contactId}")` trigger reads `ownerId` from the deleted
     document and deletes `business_cards/{ownerId}/{contactId}.jpg` with
     `{ ignoreNotFound: true }`. Fires whenever *any* contact doc is deleted — covers
     already-orphaned images and clients that die mid-delete.

## Deploy (needs Functions access)
```bash
firebase deploy --only functions:cleanupContactCardImage
# or deploy all: firebase deploy --only functions
```

## Verify (Storage console)
1. Add a contact **with** a business-card photo → confirm `business_cards/<uid>/<contactId>.jpg`
   exists in the Storage console.
2. Delete that contact in the app → the object disappears (client delete).
3. To exercise the **backstop**: delete a `contacts/{id}` document directly in the Firestore
   console (bypassing the app) → the matching Storage object is removed by the function
   (check the function logs: `Cleaned up card image …`).
4. Delete all contacts → no `business_cards/<uid>/…` objects remain for that user.
5. Turn off network mid-delete or force-kill the app after the doc delete → the image still
   gets cleaned up once the function processes the doc deletion.
