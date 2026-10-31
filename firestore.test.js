const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read recaps", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("recaps").get());
});

test("Authenticated user (Alice): can create her own recap item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recaps").doc("recap_1").set({
      userId: ALICE_UID,
      title: "Myanmar Tech Recap",
      sourceType: "video",
      myanmarSummary: "အကျဉ်းချုပ် အချက်အလက်များ",
      myanmarScript: "မြန်မာအသံထွက်ဖတ်ရန် အပြည့်အစုံ",
      voiceTone: "male_narrator",
      speechSpeed: 1.0,
      speechPitch: 1.0,
      durationSeconds: 120,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user (Bob): cannot read Alice's recap item", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("recaps").doc("recap_1").set({
      userId: ALICE_UID,
      title: "Alice Recap",
      sourceType: "video",
      myanmarSummary: "Summary",
      myanmarScript: "Script",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recaps").doc("recap_1").get());
});

test("Authenticated user (Alice): cannot create recap item with spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("recaps").doc("recap_1").set({
      userId: BOB_UID, // Spoofed!
      title: "Spoofed Recap",
      sourceType: "video",
      myanmarSummary: "Summary",
      myanmarScript: "Script",
      createdAt: new Date(),
    })
  );
});
