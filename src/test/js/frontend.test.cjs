"use strict";

const assert = require("node:assert/strict");
const { loadEsm } = require("./esm-loader.cjs");

async function run() {
  const dates = await loadEsm("src/main/resources/static/assets/js/dates.js");
  const apiMod = await loadEsm("src/main/resources/static/assets/js/api.js");
  const tasksMod = await loadEsm(
    "src/main/resources/static/assets/js/views/tasks.js"
  );
  const shoppingMod = await loadEsm(
    "src/main/resources/static/assets/js/views/shopping.js"
  );
  const eventsMod = await loadEsm(
    "src/main/resources/static/assets/js/views/events.js"
  );

  // dates.js
  assert.equal(dates.toHebrewNumber(1), "א׳");
  assert.equal(dates.toHebrewNumber(15), "ט״ו");
  assert.equal(dates.toHebrewNumber(16), "ט״ז");
  assert.equal(dates.toHebrewNumber(27), "כ״ז");
  assert.equal(dates.toHebrewNumber(787), "תשפ״ז");

  const hebrewDate = dates.formatHebrewDateFromIso("2026-11-19");
  assert.ok(hebrewDate.includes("ה׳תשפ״ז"), "formats Hebrew year");
  assert.equal(dates.formatHebrewDateFromIso(""), "");

  assert.equal(dates.dayIndex("SUNDAY"), 0);
  assert.equal(dates.dayIndex("friday"), 5);
  assert.equal(dates.dayIndex("יום שלישי"), 2);
  assert.equal(dates.dayIndex("שבת"), 6);
  assert.equal(dates.dayIndex("unknown"), -1);
  assert.equal(dates.dayLabel("MONDAY"), "יום שני");
  assert.equal(dates.dayLabel(""), "לא נקבע יום");

  assert.equal(dates.formatIsoAsDisplayDate("2026-11-19"), "19/11/2026");
  assert.equal(dates.formatIsoAsDisplayDate(""), "");

  assert.equal(dates.shiftYearMonth("2026-12", 1), "2027-01");
  assert.equal(dates.shiftYearMonth("2026-01", -1), "2025-12");
  assert.equal(dates.shiftYearMonth("0001-01", -1), "0001-01");
  assert.equal(dates.shiftYearMonth("9999-12", 1), "9999-12");

  // api.js
  assert.equal(
    apiMod.errorMessage(400, { message: "Current password is incorrect" }),
    "הסיסמה הנוכחית אינה נכונה."
  );
  assert.equal(
    apiMod.errorMessage(400, { detail: "Invalid month" }),
    "החודש שנבחר אינו תקין."
  );
  assert.equal(
    apiMod.errorMessage(400, { name: "Task name is required" }),
    "צריך למלא שם למשימה."
  );
  assert.equal(
    apiMod.errorMessage(400, {
      detail: "Validation failed",
      errors: { description: "Description is required" }
    }),
    "צריך למלא תיאור לאירוע."
  );
  for (const status of [400, 401, 403, 404, 409, 500]) {
    assert.ok(apiMod.errorMessage(status, null).length > 0);
  }

  assert.deepEqual(apiMod.unwrapList([1, 2]), [1, 2]);
  assert.deepEqual(apiMod.unwrapList({ content: [3, 4] }), [3, 4]);
  assert.deepEqual(apiMod.unwrapList(null), []);

  // tasks.js
  const user = { id: 10, username: "dvir", role: "ADMIN" };
  const assignees = [
    { id: 10, username: "dvir" },
    { id: 20, username: "dana" }
  ];
  const sampleTasks = [
    {
      id: 1,
      name: "שטיפת רצפה",
      description: "בסלון",
      type: "ONETIME",
      taskDate: "2026-11-20",
      assigneeId: 10,
      completed: true
    },
    {
      id: 2,
      name: "הוצאת אשפה",
      description: "",
      type: "WEEKLY",
      day: "FRIDAY",
      assigneeId: 20,
      completed: false
    },
    {
      id: 3,
      name: "קניית לחם",
      description: "מאפייה",
      type: "ONETIME",
      taskDate: "2026-11-19",
      assigneeId: 10,
      completed: false
    }
  ];

  assert.equal(tasksMod.taskType(sampleTasks[0]), "ONETIME");
  assert.equal(tasksMod.taskType(sampleTasks[1]), "WEEKLY");
  assert.equal(tasksMod.taskDayIndex(sampleTasks[1]), 5);
  assert.equal(
    tasksMod.taskSortDate(sampleTasks[1], "2026-11-19"),
    "2026-11-20"
  );

  const openFirst = tasksMod.filterAndSortTasks(sampleTasks, {
    today: "2026-11-19",
    user,
    assignees
  });
  assert.deepEqual(
    openFirst.map(t => t.id),
    [3, 2, 1],
    "sorts open tasks before completed and by due date"
  );

  assert.deepEqual(
    tasksMod
      .filterAndSortTasks(sampleTasks, {
        filter: "mine",
        today: "2026-11-19",
        user,
        assignees
      })
      .map(t => t.id),
    [3, 1]
  );
  assert.deepEqual(
    tasksMod
      .filterAndSortTasks(sampleTasks, {
        type: "WEEKLY",
        day: "5",
        today: "2026-11-19",
        user,
        assignees
      })
      .map(t => t.id),
    [2]
  );
  assert.deepEqual(
    tasksMod
      .filterAndSortTasks(sampleTasks, {
        query: "מאפייה",
        filter: "open",
        today: "2026-11-19",
        user,
        assignees
      })
      .map(t => t.id),
    [3]
  );

  // shopping.js
  const nowMs = Date.parse("2026-11-19T12:00:00Z");
  assert.equal(
    shoppingMod.isStaleShoppingItem(
      { updatedAt: "2026-11-10T12:00:00Z" },
      nowMs
    ),
    true
  );
  assert.equal(
    shoppingMod.isStaleShoppingItem(
      { updatedAt: "2026-11-18T12:00:00Z" },
      nowMs
    ),
    false
  );
  assert.equal(
    shoppingMod.isStaleShoppingItem({ updatedAt: "invalid" }, nowMs),
    false
  );

  const filteredShopping = shoppingMod.filterShoppingItems(
    [
      { id: 1, productName: "חלב שקדים" },
      { id: 2, productName: "לחם" }
    ],
    "חלב"
  );
  assert.deepEqual(
    filteredShopping.map(item => item.id),
    [1]
  );

  // events.js
  const sortedEvents = eventsMod.filterAndSortEvents(
    [
      { id: 2, description: "יומולדת", eventDate: "2026-12-01" },
      { id: 1, description: "ארוחת ערב", eventDate: "2026-11-21" }
    ],
    ""
  );
  assert.deepEqual(
    sortedEvents.map(event => event.id),
    [1, 2]
  );
  assert.deepEqual(
    eventsMod.filterAndSortEvents(sortedEvents, "יומולדת").map(e => e.id),
    [2]
  );

  assert.deepEqual(
    eventsMod.resolveInviteeNames([10, 99], assignees),
    ["dvir", "משתמש 99"]
  );
  assert.deepEqual(eventsMod.resolveInviteeNames(null, assignees), []);

  // dates.getWeekDays & tasks.buildWeeklySchedule
  const weekDays = dates.getWeekDays("2026-11-25"); // Wednesday -> Sunday 22..Saturday 28
  assert.equal(weekDays.length, 7);
  assert.equal(weekDays[0].isoDate, "2026-11-22");
  assert.equal(weekDays[0].dayValue, "SUNDAY");
  assert.equal(weekDays[3].isToday, true);
  assert.equal(weekDays[6].isoDate, "2026-11-28");

  const scheduleTasks = [
    {
      id: 1,
      name: "כביסה",
      type: "WEEKLY",
      day: "SUNDAY",
      assigneeId: 10,
      completed: false
    },
    {
      id: 2,
      name: "קנייה לבית",
      type: "ONETIME",
      taskDate: "2026-11-25",
      assigneeId: 10,
      completed: true
    },
    {
      id: 3,
      name: "שבוע הבא",
      type: "ONETIME",
      taskDate: "2026-11-29",
      assigneeId: 20,
      completed: false
    },
    {
      id: 4,
      name: "משימה יתומה",
      type: "WEEKLY",
      day: "TUESDAY",
      assigneeId: null,
      completed: false
    }
  ];

  const schedule = tasksMod.buildWeeklySchedule(scheduleTasks, assignees, {
    today: "2026-11-25",
    currentUserId: 10
  });
  assert.equal(schedule.totalScheduled, 3, "excludes next-week one-time task");
  assert.equal(schedule.rows.length, 3, "dana, dvir, and unassigned");
  const dvirRow = schedule.rows.find(r => r.username === "dvir");
  assert.equal(dvirRow.isCurrentUser, true);
  assert.equal(dvirRow.cells[0][0].id, 1, "weekly task on Sunday");
  assert.equal(dvirRow.cells[3][0].id, 2, "one-time task on Wednesday");
  const danaRow = schedule.rows.find(r => r.username === "dana");
  assert.equal(danaRow.totalCount, 0);
  const unassignedRow = schedule.rows.find(r => r.isUnassigned);
  assert.equal(unassignedRow.cells[2][0].id, 4);

  console.log("Frontend unit checks passed");
}

run().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
