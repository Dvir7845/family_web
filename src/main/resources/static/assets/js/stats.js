import { israelDateKey, israelToday } from "./dates.js";
import { $, $$, setBoardNote } from "./dom.js";
import { canManageTasks, state } from "./state.js";
import { isMyTask, taskAssignee } from "./views/tasks.js";

export const VIEW_META = {
  tasks: {
    title: "משימות הבית",
    eyebrow: "קצת סדר, הרבה ביחד",
    description:
      "משימות חד־פעמיות לפי תאריך ומשימות שחוזרות בכל שבוע, במקום אחד.",
    boardTitle: "מה צריך לעשות?",
    actionLabel: "הוספת משימה",
    searchPlaceholder: "חיפוש משימה או אחראי…",
    noteIcon: "users",
    noteText: "כולם רואים את הלוח. האחראי למשימה מסמן ביצוע."
  },
  shopping: {
    title: "רשימת הקניות",
    eyebrow: "נזכרים ביחד, קונים בנחת",
    description: "מה חסר בבית? מוסיפים כאן, וכל המשפחה רואה.",
    boardTitle: "מה צריך לקנות?",
    actionLabel: "הוספת מוצר",
    searchPlaceholder: "חיפוש מוצר ברשימה…",
    noteIcon: "clock",
    noteText:
      "מוצרים שלא עודכנו שבוע מוסרים בניקוי היומי. כולם יכולים להוסיף ולעדכן."
  },
  events: {
    title: "אירועים משפחתיים",
    eyebrow: "נפגשים, חוגגים, זוכרים ביחד",
    description: "כל האירועים המשפחתיים במקום אחד.",
    boardTitle: "מה קורה במשפחה?",
    actionLabel: "הוספת אירוע",
    searchPlaceholder: "חיפוש אירוע…",
    noteIcon: "calendar",
    noteText:
      "כולם יכולים להוסיף ולעדכן אירועים. אירועים שעברו לפני יותר מ־30 יום מוסרים בניקוי היומי."
  },
  vehicles: {
    title: "מי משתמש ברכב",
    eyebrow: "מתכננים את הנסיעות ביחד",
    description: "בחרו רכב וחודש ותפסו משבצת פנויה על שמכם.",
    boardTitle: "לוח שימוש חודשי",
    actionLabel: "הוספת רכב",
    searchPlaceholder: "",
    noteIcon: "car",
    noteText: "בוקר · צהריים · ערב. רק המזמין או מנהל יכולים לבטל הזמנה."
  }
};

const numberFormatter = new Intl.NumberFormat("he-IL");

export function syncNavigation(activeView) {
  $$("[data-view]").forEach(button => {
    const active = button.dataset.view === activeView;
    button.classList.toggle("active", active);
    if (active) {
      button.setAttribute("aria-current", "page");
    } else {
      button.removeAttribute("aria-current");
    }
  });
}

export function renderViewChrome(currentState = state) {
  const view = currentState.view in VIEW_META ? currentState.view : "tasks";
  const meta = VIEW_META[view];
  const isTasks = view === "tasks";

  document.title = `${meta.title} · ביחד בבית`;
  syncNavigation(view);

  $("#pageEyebrow").textContent = meta.eyebrow;
  $("#pageTitle").textContent = meta.title;
  $("#pageDescription").textContent = meta.description;
  $("#boardTitle").textContent = meta.boardTitle;

  $("#primaryAction").hidden =
    !currentState.loaded || (isTasks && !canManageTasks(currentState.user));
  $("#primaryAction span").textContent = meta.actionLabel;

  $("#taskFilters").hidden = !isTasks;
  $("#dayFilter").hidden = !isTasks;
  $("#taskTypeFilter").hidden = !isTasks;
  $("#searchInput").placeholder = meta.searchPlaceholder;
  setBoardNote(meta.noteIcon, meta.noteText);

  $$("[data-filter]").forEach(button => {
    const selected = button.dataset.filter === currentState.filter;
    button.classList.toggle("selected", selected);
    button.setAttribute("aria-pressed", String(selected));
  });
}

export function renderStats(currentState = state) {
  let values;
  const today = israelToday();

  if (currentState.view === "events") {
    const userId = currentState.user?.id;
    values = [
      ["אירועים בלוח", currentState.events.length, "calendar"],
      [
        "מהיום והלאה",
        currentState.events.filter(event => event.eventDate >= today).length,
        "tasks"
      ],
      [
        "אירועים שהוזמנתי אליהם",
        currentState.events.filter(
          event =>
            Array.isArray(event.invitedUserIds) &&
            event.invitedUserIds.includes(userId)
        ).length,
        "users"
      ]
    ];
  } else if (currentState.view === "shopping") {
    values = [
      ["מוצרים ברשימה", currentState.shopping.length, "shopping"],
      [
        "יחידות לקנייה",
        currentState.shopping.reduce((sum, item) => sum + item.quantity, 0),
        "plus"
      ],
      [
        "עודכנו היום",
        currentState.shopping.filter(
          item => israelDateKey(item.updatedAt) === today
        ).length,
        "clock"
      ]
    ];
  } else {
    const hasMissingAssignee = currentState.tasks.some(task => {
      if (task.completed) return false;
      const assignee = taskAssignee(task, currentState.assignees);
      return !assignee.username && assignee.id === null;
    });

    values = [
      [
        "משימות פתוחות",
        currentState.tasks.filter(task => !task.completed).length,
        "tasks"
      ],
      [
        "באחריותי",
        hasMissingAssignee
          ? null
          : currentState.tasks.filter(
              task =>
                !task.completed &&
                isMyTask(task, currentState.user, currentState.assignees)
            ).length,
        "users"
      ],
      [
        "כבר הושלמו",
        currentState.tasks.filter(task => task.completed).length,
        "check"
      ]
    ];
  }

  values.forEach(([label, value, glyph], index) => {
    $(`#statLabel${index + 1}`).textContent = label;
    $(`#statValue${index + 1}`).textContent =
      value === null ? "—" : numberFormatter.format(value);
    $(`#statIcon${index + 1}`).setAttribute("href", `/assets/icons.svg#${glyph}`);
  });
}
