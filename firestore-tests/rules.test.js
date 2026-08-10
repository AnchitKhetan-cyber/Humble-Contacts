/**
 * Security-rules tests for firestore.rules, run against the Firestore emulator.
 *
 *   cd firestore-tests && npm install
 *   npm test        # -> firebase emulators:exec --only firestore "node rules.test.js"
 *
 * Covers, per collection: owner allowed, non-owner denied, unauthenticated denied,
 * for both reads and writes. Exits non-zero if any expectation fails.
 */

const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  getDoc,
  setDoc,
  deleteDoc,
} = require("firebase/firestore");

const PROJECT_ID = "demo-humble-contacts";
const ALICE = "alice-uid";
const BOB = "bob-uid";

let passed = 0;
let failed = 0;

async function check(name, promise) {
  try {
    await promise;
    console.log(`  ✓ ${name}`);
    passed++;
  } catch (err) {
    console.error(`  ✗ ${name}\n      ${err.message}`);
    failed++;
  }
}

async function main() {
  const testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, "../firestore.rules"), "utf8"),
    },
  });

  // Seed data with rules bypassed so tests start from a known state.
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "contacts/c_alice"), { ownerId: ALICE, fullName: "A's contact" });
    await setDoc(doc(db, "contacts/c_bob"), { ownerId: BOB, fullName: "B's contact" });
    await setDoc(doc(db, "users/" + ALICE), { name: "Alice" });
    await setDoc(doc(db, "users/" + BOB), { name: "Bob" });
    await setDoc(doc(db, "account_deletions/" + ALICE), { token: "t", confirmed: false });
  });

  const alice = testEnv.authenticatedContext(ALICE).firestore();
  const bob = testEnv.authenticatedContext(BOB).firestore();
  const anon = testEnv.unauthenticatedContext().firestore();

  console.log("contacts/{id} (owner via ownerId field)");
  await check("owner reads own contact", assertSucceeds(getDoc(doc(alice, "contacts/c_alice"))));
  await check("non-owner CANNOT read another's contact", assertFails(getDoc(doc(bob, "contacts/c_alice"))));
  await check("anonymous CANNOT read a contact", assertFails(getDoc(doc(anon, "contacts/c_alice"))));
  await check("owner creates a contact stamped to self",
    assertSucceeds(setDoc(doc(alice, "contacts/c_new"), { ownerId: ALICE, fullName: "new" })));
  await check("CANNOT create a contact owned by someone else",
    assertFails(setDoc(doc(alice, "contacts/c_evil"), { ownerId: BOB, fullName: "evil" })));
  await check("non-owner CANNOT update another's contact",
    assertFails(setDoc(doc(bob, "contacts/c_alice"), { ownerId: ALICE, fullName: "hacked" })));
  await check("non-owner CANNOT delete another's contact",
    assertFails(deleteDoc(doc(alice, "contacts/c_bob"))));
  await check("owner deletes own contact", assertSucceeds(deleteDoc(doc(alice, "contacts/c_alice"))));

  console.log("users/{uid} (owner via document id)");
  await check("owner reads own user doc", assertSucceeds(getDoc(doc(alice, "users/" + ALICE))));
  await check("other user CANNOT read your user doc", assertFails(getDoc(doc(bob, "users/" + ALICE))));
  await check("anonymous CANNOT read a user doc", assertFails(getDoc(doc(anon, "users/" + ALICE))));
  await check("owner writes own user doc",
    assertSucceeds(setDoc(doc(alice, "users/" + ALICE), { name: "Alice 2" })));
  await check("other user CANNOT write your user doc",
    assertFails(setDoc(doc(bob, "users/" + ALICE), { name: "hacked" })));

  console.log("account_deletions/{uid} (read/delete own; writes are server-side)");
  await check("owner reads own deletion doc", assertSucceeds(getDoc(doc(alice, "account_deletions/" + ALICE))));
  await check("other user CANNOT read your deletion doc", assertFails(getDoc(doc(bob, "account_deletions/" + ALICE))));
  await check("client CANNOT create a deletion doc",
    assertFails(setDoc(doc(alice, "account_deletions/" + ALICE), { token: "forged" })));
  await check("owner deletes own deletion doc", assertSucceeds(deleteDoc(doc(alice, "account_deletions/" + ALICE))));

  console.log("catch-all");
  await check("unknown collection is denied", assertFails(getDoc(doc(alice, "secrets/x"))));

  await testEnv.cleanup();

  console.log(`\n${passed} passed, ${failed} failed`);
  if (failed > 0) process.exit(1);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
