const { test, before, after, beforeEach } = require('node:test');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('fs');

const PROJECT_ID = 'demo-no-project';
const DB_ID = 'ai-studio-android-dairysay-da7f17ed-cb03-4797-b856-919781eeadd4';
const ALICE_UID = 'partner_a_alice';
const BOB_UID = 'partner_b_bob';
const CHARLIE_UID = 'stranger_charlie';

let testEnv;

before(async () => {
  const rules = fs.readFileSync('./firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: '127.0.0.1',
      port: 8085,
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

test('Unauthenticated user cannot read diary entries', async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection('diaries').doc('diary_1').collection('entries').get()
  );
});

test('Partner A can create a diary and add entries', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Create diary
  await assertSucceeds(
    aliceDb.collection('diaries').doc('diary_1').set({
      name: 'Dairy Sayangg',
      ownerUid: ALICE_UID,
      partnerUid: '',
      inviteCode: 'DS-123456',
      memberUids: [ALICE_UID],
      partnerAName: 'Alice',
      partnerBName: ''
    })
  );

  // Add diary entry
  await assertSucceeds(
    aliceDb.collection('diaries').doc('diary_1').collection('entries').doc('entry_1').set({
      diaryId: 'diary_1',
      title: 'Hari Pertama',
      content: 'Kita jadian hari ini!',
      authorUid: ALICE_UID,
      authorDisplayName: 'Alice',
      memberUids: [ALICE_UID]
    })
  );
});

test('Unauthorized user Charlie cannot read Alice and Bob shared diary', async () => {
  // Seed diary as admin
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection('diaries').doc('diary_ab').set({
      name: 'Dairy Sayangg',
      ownerUid: ALICE_UID,
      partnerUid: BOB_UID,
      inviteCode: 'DS-999999',
      memberUids: [ALICE_UID, BOB_UID]
    });
    await db.collection('diaries').doc('diary_ab').collection('entries').doc('entry_secret').set({
      diaryId: 'diary_ab',
      title: 'Rahasia Kita',
      content: 'Hanya berdua yang tahu',
      authorUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID]
    });
  });

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Charlie tries to read Alice & Bob's entry
  await assertFails(
    charlieDb.collection('diaries').doc('diary_ab').collection('entries').doc('entry_secret').get()
  );

  // Charlie tries to write into Alice & Bob's diary
  await assertFails(
    charlieDb.collection('diaries').doc('diary_ab').collection('entries').doc('charlie_entry').set({
      diaryId: 'diary_ab',
      title: 'Hacked',
      content: 'Spy',
      authorUid: CHARLIE_UID,
      memberUids: [ALICE_UID, BOB_UID]
    })
  );
});

test('Love letters: Partner B cannot read Partner A private draft, but can read published letters', async () => {
  // Seed draft letter and published letter
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection('diaries').doc('diary_ab').set({
      ownerUid: ALICE_UID,
      partnerUid: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID]
    });
    await db.collection('diaries').doc('diary_ab').collection('letters').doc('draft_alice').set({
      diaryId: 'diary_ab',
      title: 'Draft Kejutan',
      content: 'Belum boleh dibaca Bob',
      template: 'Open When You Miss Me',
      isDraft: true,
      authorUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID]
    });
    await db.collection('diaries').doc('diary_ab').collection('letters').doc('shared_alice').set({
      diaryId: 'diary_ab',
      title: 'Surat Terbuka',
      content: 'Untuk Bob tersayang',
      template: 'Open When You Miss Me',
      isDraft: false,
      authorUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID]
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Bob cannot read Alice's private draft
  await assertFails(
    bobDb.collection('diaries').doc('diary_ab').collection('letters').doc('draft_alice').get()
  );

  // Bob CAN read Alice's published letter
  await assertSucceeds(
    bobDb.collection('diaries').doc('diary_ab').collection('letters').doc('shared_alice').get()
  );
});
