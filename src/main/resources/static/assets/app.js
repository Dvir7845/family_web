"use strict";

const $ = selector => document.querySelector(selector);
const state = {
  view: ["tasks", "shopping", "events", "vehicles"].includes(location.hash.slice(1))
    ? location.hash.slice(1)
    : "tasks",
  user: null,
  vehicles: [], bookings: [], vehicleId: "",
  vehicleMonth: new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Jerusalem", year: "numeric", month: "2-digit" }).format(new Date()),
  tasks: [],
  shopping: [],
  events: [],
  assignees: [],
  filter: "all",
  loaded: false,
  loading: false,
  pendingDelete: null
};
const dayNames = ["ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"];
const dayValues = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
const dateFormatter = new Intl.DateTimeFormat("he-IL", {
  timeZone: "Asia/Jerusalem", dateStyle: "short", timeStyle: "short"
});
const hebrewDateFormatter = new Intl.DateTimeFormat(
  "he-IL-u-ca-hebrew",
  {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "UTC"
  }
);
let toastTimer;

function icon(name) {
  return `<svg class="icon" aria-hidden="true"><use href="/assets/icons.svg#${name}"></use></svg>`;
}

function isAdmin() {
  return state.user?.role === "ADMIN";
}

function errorMessage(status, data) {
  const translations = {
    "Current password is incorrect": "הסיסמה הנוכחית אינה נכונה.",
    "Cannot delete your own account": "לא ניתן למחוק את החשבון שבו מחוברים.",
    "Task name is required": "צריך למלא שם למשימה.",
    "Day is required": "צריך לבחור יום בשבוע.",
    "Assignee is required": "צריך לבחור אחראי למשימה.",
    "Product name is required": "צריך למלא שם מוצר.",
    "Quantity must be greater than zero": "הכמות חייבת להיות מספר שלם גדול מאפס.",
    "Username already exists": "שם המשתמש הזה כבר קיים. בחרו שם אחר.",
    "Selected assignee does not exist": "האחראי שנבחר אינו קיים. רעננו את רשימת בני המשפחה.",
    "Password exceeds the supported byte length": "הסיסמה ארוכה מדי. נסו סיסמה קצרה יותר."
  };
  if (data?.message && translations[data.message]) return translations[data.message];
  const fieldNames = new Set(["name", "day", "assigneeId", "productName", "quantity", "username", "password", "currentPassword", "newPassword"]);
  if (status === 400 && data && typeof data === "object") {
    const messages = Object.entries(data)
      .filter(([key, value]) => fieldNames.has(key) && typeof value === "string")
      .map(([, value]) => translations[value] || value);
    if (messages.length) return messages.join(" ");
  }
  return ({
    400: "חלק מהפרטים אינם תקינים. בדקו את השדות ונסו שוב.",
    401: "ההתחברות הסתיימה. צריך להיכנס שוב.",
    403: "אין הרשאה לפעולה הזאת, או שההתחברות השתנתה. נסו לרענן את העמוד.",
    404: "הפריט כבר לא נמצא או שהפעולה אינה זמינה עבורך. כדאי לרענן את הרשימה.",
    409: "הפרטים כבר תפוסים. ייתכן שהמשבצת הוזמנה בינתיים; רעננו ונסו שוב."
  })[status] || "לא הצלחנו להשלים את הפעולה. נסו שוב בעוד רגע.";
}

async function getCsrf() {
  const response = await fetch("/csrf", {
    credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" }
  });
  if (!response.ok) throw new Error("לא הצלחנו להכין את החיבור. נסו לרענן את העמוד.");
  const token = await response.json();
  if (!token.token || !token.headerName) throw new Error("לא הצלחנו להכין את החיבור. נסו לרענן את העמוד.");
  return token;
}

async function api(path, method = "GET", body) {
  const headers = { Accept: "application/json" };
  if (method !== "GET") {
    const csrf = await getCsrf();
    headers[csrf.headerName] = csrf.token;
  }
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let response;
  try {
    response = await fetch(path, {
      method, headers, credentials: "same-origin", cache: "no-store",
      ...(body !== undefined ? { body: JSON.stringify(body) } : {})
    });
  } catch {
    throw new Error("אין כרגע חיבור לשרת. בדקו שהוא פועל ונסו לרענן.");
  }
  if (response.status === 401 || (response.redirected && new URL(response.url).pathname === "/login.html")) {
    location.assign("/login.html?expired");
    throw new Error("ההתחברות הסתיימה.");
  }
  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { /* Some server failures have no JSON body. */ }
  }
  if (!response.ok) throw new Error(errorMessage(response.status, data));
  return data;
}

function notify(message, failed = false) {
  const toast = $("#toast");
  clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.toggle("failed", failed);
  toast.hidden = false;
  toastTimer = setTimeout(() => { toast.hidden = true; }, 4500);
}

function showPageError(error) {
  $("#pageError").textContent = error.message || "לא הצלחנו לטעון את הלוח.";
  $("#pageError").hidden = false;
}

function dayIndex(day) {
  const text = String(day || "").trim().replace(/^יום\s+/, "");
  const english = dayValues.findIndex(value => value.toLowerCase() === text.toLowerCase());
  return english >= 0 ? english : dayNames.indexOf(text);
}

function dayLabel(day) {
  const index = dayIndex(day);
  return index >= 0 ? `יום ${dayNames[index]}` : (day || "לא נקבע יום");
}

function avatar(username) {
  const element = document.createElement("span");
  element.className = "avatar";
  element.setAttribute("aria-hidden", "true");
  element.textContent = String(username || "?").slice(0, 1).toUpperCase();
  const hue = [...String(username || "")].reduce((sum, char) => sum + char.charCodeAt(0), 0) % 100;
  element.style.backgroundColor = `hsl(${hue + 40} 23% 90%)`;
  element.style.color = `hsl(${hue + 40} 22% 40%)`;
  return element;
}

function actionButton(name, label, onClick, danger = false) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `icon-button${danger ? " danger" : ""}`;
  button.innerHTML = icon(name);
  button.setAttribute("aria-label", label);
  button.title = label;
  button.addEventListener("click", onClick);
  return button;
}

function textElement(tag, className, text) {
  const element = document.createElement(tag);
  element.className = className;
  element.textContent = text;
  return element;
}

async function loadData(initial = false) {
  if (state.loading) return;
  state.loading = true;
  $("#refreshButton").disabled = true;
  $("#vehicleMonth").disabled = true;
  $("#pageError").hidden = true;
  if (initial || !state.loaded) {
    $("#loadingState").hidden = false;
    $("#emptyState").hidden = true;
    $("#tableContainer").hidden = true;
  }
  try {
    const user = await api("/me");
    const [tasks, shopping, assignees, events] = await Promise.all([
      api("/tasks"),
      api("/shopping-items"),
      api("/members"),
      api("/events")
    ]);
    state.user = user;
    $("#vehicleNavigation").hidden = !user.driver;
    if (!user.driver && state.view === "vehicles") state.view = "tasks";
    if (user.driver) {
      [state.vehicles, state.bookings] = await Promise.all([api("/vehicles"), api(`/vehicles/bookings?month=${state.vehicleMonth}`)]);
    } else { state.vehicles = []; state.bookings = []; }
    state.tasks = tasks;
    state.shopping = shopping;
    state.assignees = assignees;
    state.events = events;
    state.loaded = true;
    $("#currentUsername").textContent = user.username;
    $("#currentRole").textContent = isAdmin() ? "מנהל המשימות" : "בן / בת משפחה";
    $("#userAvatar").replaceWith(Object.assign(avatar(user.username), { id: "userAvatar" }));
    $("#adminNavigation").hidden = !isAdmin();
    $("#navTaskCount").textContent = tasks.filter(task => !task.completed).length;
    $("#navTaskCount").title = "משימות פתוחות";
    $("#navShoppingCount").textContent = shopping.length;
    $("#navEventCount").textContent = events.length;
    $("#lastSynced").textContent = `רוענן ב־${new Intl.DateTimeFormat("he-IL", { hour: "2-digit", minute: "2-digit" }).format(new Date())}`;
    render();
  } catch (error) {
    showPageError(error);
  } finally {
    state.loading = false;
    $("#loadingState").hidden = true;
    $("#refreshButton").disabled = false;
    $("#vehicleMonth").disabled = false;
  }
}

function setView(view) {
  if (view === "vehicles" && !state.user?.driver) return;
  if (state.view !== view) {
    state.filter = "all";
    $("#searchInput").value = "";
    $("#dayFilter").value = "all";
  }
  state.view = view;
  history.replaceState(null, "", `#${view}`);
  render();
}

function render() {
  const vehicles = state.view === "vehicles" && state.user?.driver;
  $("#vehicleBoard").hidden = !vehicles;
  $(".board-toolbar").hidden = vehicles;
  $(".stats-grid").hidden = vehicles;
  if (vehicles) { renderVehicles(); return; }
  const shopping = state.view === "shopping";
  const events = state.view === "events";
  const tasks = state.view === "tasks";

  const title = events
    ? "אירועים משפחתיים"
    : shopping ? "רשימת הקניות" : "משימות הבית";

  const actionLabel = events
    ? "הוספת אירוע"
    : shopping ? "הוספת מוצר" : "הוספת משימה";

  document.title = `${title} · ביחד בבית`;

  document.querySelectorAll("[data-view]").forEach(button => {
    const active = button.dataset.view === state.view;
    button.classList.toggle("active", active);

    if (active) {
      button.setAttribute("aria-current", "page");
    } else {
      button.removeAttribute("aria-current");
    }
  });

  $("#pageEyebrow").textContent = events
    ? "נפגשים, חוגגים, זוכרים ביחד"
    : shopping ? "נזכרים ביחד, קונים בנחת" : "קצת סדר, הרבה ביחד";

  $("#pageTitle").textContent = title;

  $("#pageDescription").textContent = events
    ? "כל האירועים המשפחתיים במקום אחד."
    : shopping
      ? "מה חסר בבית? מוסיפים כאן, וכל המשפחה רואה."
      : "כל המשימות במקום אחד. כל אחד יודע מה החלק שלו.";

  $("#boardTitle").textContent = events
    ? "מה קורה במשפחה?"
    : shopping ? "מה צריך לקנות?" : "מה עושים השבוע?";

  $("#primaryAction").hidden =
    !state.loaded || (tasks && !isAdmin());

  $("#primaryAction span").textContent = actionLabel;

  $("#taskFilters").hidden = !tasks;
  $("#dayFilter").hidden = !tasks;

  $("#searchInput").placeholder = events
    ? "חיפוש אירוע…"
    : shopping ? "חיפוש מוצר ברשימה…" : "חיפוש משימה או אחראי…";

  $("#boardNote").innerHTML = events
    ? `${icon("users")}<span>כולם יכולים להוסיף ולעדכן אירועים. אירועים שעברו לפני יותר מ־30 יום מוסרים בניקוי היומי.</span>`
    : shopping
      ? `${icon("clock")}<span>מוצרים שלא עודכנו שבוע מוסרים בניקוי היומי. כולם יכולים להוסיף ולעדכן.</span>`
      : `${icon("users")}<span>כולם רואים את הלוח. האחראי למשימה מסמן ביצוע.</span>`;

  document.querySelectorAll("[data-filter]").forEach(button => {
    const selected = button.dataset.filter === state.filter;
    button.classList.toggle("selected", selected);
    button.setAttribute("aria-pressed", String(selected));
  });

  if (!state.loaded) return;

  renderStats();

  if (events) {
    renderEvents();
  } else if (shopping) {
    renderShopping();
  } else {
    renderTasks();
  }
}

function renderStats() {
  let values;

  if (state.view === "events") {
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone: "Asia/Jerusalem",
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    }).formatToParts(new Date());

    const dateParts = Object.fromEntries(
      parts.map(part => [part.type, part.value])
    );

    const today =
      `${dateParts.year}-${dateParts.month}-${dateParts.day}`;

    values = [
      ["אירועים בלוח", state.events.length, "clock"],
      [
        "מהיום והלאה",
        state.events.filter(event => event.eventDate >= today).length,
        "tasks"
      ],
      [
        "אירועים שהוזמנתי אליהם",
        state.events.filter(event =>
          event.invitedUserIds.includes(state.user.id)
        ).length,
        "users"
      ]
    ];
  } else if (state.view === "shopping") {
    const today = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Jerusalem" });
    const day = today.format(new Date());
    values = [
      ["מוצרים ברשימה", state.shopping.length, "shopping"],
      ["יחידות לקנייה", state.shopping.reduce((sum, item) => sum + item.quantity, 0), "plus"],
      ["עודכנו היום", state.shopping.filter(item => today.format(new Date(item.updatedAt)) === day).length, "clock"]
    ];
  } else {
    values = [
      ["משימות פתוחות", state.tasks.filter(task => !task.completed).length, "tasks"],
      ["באחריותי", state.tasks.filter(task => !task.completed && task.assigneeUsername === state.user.username).length, "users"],
      ["כבר הושלמו", state.tasks.filter(task => task.completed).length, "check"]
    ];
  }
  values.forEach(([label, value, glyph], index) => {
    $("#statLabel" + (index + 1)).textContent = label;
    $("#statValue" + (index + 1)).textContent = new Intl.NumberFormat("he-IL").format(value);
    $("#statIcon" + (index + 1)).setAttribute("href", `/assets/icons.svg#${glyph}`);
  });
}

function renderTasks() {
  const query = $("#searchInput").value.trim().toLowerCase();
  const day = $("#dayFilter").value;
  const tasks = state.tasks.filter(task => {
    const searchable = `${task.name} ${task.description || ""} ${task.assigneeUsername || ""}`.toLowerCase();
    return (!query || searchable.includes(query))
      && (day === "all" || dayIndex(task.day) === Number(day))
      && (state.filter !== "mine" || task.assigneeUsername === state.user.username)
      && (state.filter !== "open" || !task.completed)
      && (state.filter !== "done" || task.completed);
  }).sort((a, b) => {
    const first = dayIndex(a.day), second = dayIndex(b.day);
    return (first < 0 ? 7 : first) - (second < 0 ? 7 : second)
      || Number(a.completed) - Number(b.completed) || a.id - b.id;
  });
  $("#tableHead").innerHTML = "<tr><th scope='col'>המשימה</th><th scope='col'>יום</th><th scope='col'>באחריות</th><th scope='col'>מצב</th><th scope='col'><span class='sr-only'>פעולות</span></th></tr>";
  const body = $("#tableBody");
  body.replaceChildren();
  tasks.forEach(task => {
    const row = document.createElement("tr");
    row.classList.toggle("completed-row", task.completed);
    row.innerHTML = "<td class='item-main'></td><td data-label='יום'></td><td data-label='באחריות'></td><td data-label='מצב'></td><td class='item-actions'><div class='row-actions'></div></td>";
    row.children[0].append(textElement("span", "item-title", task.name));
    if (task.description) row.children[0].append(textElement("span", "item-description", task.description));
    row.children[1].append(textElement("span", "day-tag", dayLabel(task.day)));
    const person = document.createElement("span");
    person.className = "person";
    if (task.assigneeUsername) person.append(avatar(task.assigneeUsername));
    person.append(textElement("span", "", task.assigneeUsername || "טרם הוקצה"));
    row.children[2].append(person);
    row.children[3].append(textElement("span", `badge${task.completed ? " done" : ""}`, task.completed ? "הושלמה" : "לביצוע"));
    const actions = row.querySelector(".row-actions");
    if (!task.completed && task.assigneeUsername === state.user.username) {
      const complete = document.createElement("button");
      complete.type = "button";
      complete.className = "complete-button";
      complete.innerHTML = `${icon("check")}<span>סיימתי</span>`;
      complete.setAttribute("aria-label", `סימון המשימה ${task.name} כהושלמה`);
      complete.addEventListener("click", () => runButtonAction(complete, async () => {
        await api(`/tasks/${task.id}/complete`, "PATCH");
        notify("עוד משימה מאחורינו. תודה על החלק שלך!");
        await loadData();
      }));
      actions.append(complete);
    } else if (task.completed && !isAdmin()) {
      const done = textElement("span", "completed-note", "בוצע");
      done.prepend(Object.assign(document.createElement("span"), { innerHTML: icon("check") }));
      actions.append(done);
    }
    if (isAdmin()) {
      actions.append(actionButton("edit", `עריכת המשימה ${task.name}`, () => openTask(task)));
      actions.append(actionButton("trash", `מחיקת המשימה ${task.name}`, () => confirmDelete(
        "למחוק את המשימה?", `המשימה ״${task.name}״ תוסר מהלוח של כל המשפחה.`, async () => {
          await api(`/tasks/${task.id}`, "DELETE");
          notify("המשימה הוסרה מהלוח.");
        }
      ), true));
    }
    body.append(row);
  });
  $("#boardCount").textContent = `${tasks.length} משימות מוצגות · ${state.tasks.length} בסך הכול`;
  showTableOrEmpty(tasks.length, Boolean(query || day !== "all" || state.filter !== "all"));
}

function renderShopping() {
  const query = $("#searchInput").value.trim().toLowerCase();
  const items = state.shopping.filter(item => item.productName.toLowerCase().includes(query));
  $("#tableHead").innerHTML = "<tr><th scope='col'>המוצר</th><th scope='col'>כמות</th><th scope='col'>עדכון אחרון</th><th scope='col'><span class='sr-only'>פעולות</span></th></tr>";
  const body = $("#tableBody");
  body.replaceChildren();
  items.forEach(item => {
    const row = document.createElement("tr");
    row.innerHTML = "<td class='item-main'></td><td data-label='כמות'></td><td class='date-cell' data-label='עדכון אחרון'></td><td class='item-actions'><div class='row-actions'></div></td>";
    row.children[0].append(textElement("span", "item-title", item.productName));
    row.children[1].append(textElement("span", "quantity-badge", item.quantity));
    const date = new Date(item.updatedAt);
    const time = textElement("time", "", Number.isNaN(date.getTime()) ? "—" : dateFormatter.format(date));
    time.dateTime = item.updatedAt;
    row.children[2].append(time);
    if (Date.now() - date.getTime() >= 7 * 24 * 60 * 60 * 1000) {
      row.children[2].append(textElement("small", "", "לא עודכן שבוע · ממתין לניקוי"));
    }
    const actions = row.querySelector(".row-actions");
    actions.append(actionButton("edit", `עדכון המוצר ${item.productName}`, () => openShopping(item)));
    actions.append(actionButton("trash", `מחיקת המוצר ${item.productName}`, () => confirmDelete(
      "להסיר מרשימת הקניות?", `המוצר ״${item.productName}״ יוסר מהרשימה המשותפת.`, async () => {
        await api(`/shopping-items/${item.id}`, "DELETE");
        notify("המוצר הוסר מרשימת הקניות.");
      }
    ), true));
    body.append(row);
  });
  $("#boardCount").textContent = `${items.length} מוצרים מוצגים · ${state.shopping.length} בסך הכול`;
  showTableOrEmpty(items.length, Boolean(query));
}

function toHebrewNumber(number) {
  const letters = [
    [400, "ת"], [300, "ש"], [200, "ר"], [100, "ק"],
    [90, "צ"], [80, "פ"], [70, "ע"], [60, "ס"],
    [50, "נ"], [40, "מ"], [30, "ל"], [20, "כ"],
    [10, "י"], [9, "ט"], [8, "ח"], [7, "ז"],
    [6, "ו"], [5, "ה"], [4, "ד"], [3, "ג"],
    [2, "ב"], [1, "א"]
  ];

  let result = "";

  for (const [value, letter] of letters) {
    if (number === 15 || number === 16) {
      result += number === 15 ? "טו" : "טז";
      break;
    }

    while (number >= value) {
      result += letter;
      number -= value;
    }
  }

  if (result.length === 1) {
    return result + "׳";
  }

  return result.slice(0, -1) + "״" + result.slice(-1);
}

function formatHebrewDate(date) {
  const parts = Object.fromEntries(
    hebrewDateFormatter.formatToParts(date)
      .map(part => [part.type, part.value])
  );

  const year = Number(parts.year);
  const thousands = Math.floor(year / 1000);
  const remainder = year % 1000;

  const hebrewDay = toHebrewNumber(Number(parts.day));
  const hebrewYear =
    toHebrewNumber(thousands) +
    (remainder ? toHebrewNumber(remainder) : "");

  return `${hebrewDay} ב${parts.month} ${hebrewYear}`;
}

function renderEvents() {
  const query = $("#searchInput").value.trim().toLowerCase();

  const events = state.events
    .filter(event => event.description.toLowerCase().includes(query))
    .sort((a, b) =>
      a.eventDate.localeCompare(b.eventDate) || a.id - b.id
    );

  $("#tableHead").innerHTML = `
    <tr>
      <th scope="col">האירוע</th>
      <th scope="col">תאריך</th>
      <th scope="col">מוזמנים</th>
      <th scope="col">
        <span class="sr-only">פעולות</span>
      </th>
    </tr>
  `;

  const body = $("#tableBody");
  body.replaceChildren();

  events.forEach(event => {
    const row = document.createElement("tr");

    row.innerHTML = `
      <td class="item-main"></td>
      <td data-label="תאריך"></td>
      <td data-label="מוזמנים"></td>
      <td class="item-actions">
        <div class="row-actions"></div>
      </td>
    `;

    row.children[0].append(
      textElement("span", "item-title", event.description)
    );

    const [year, month, day] = event.eventDate.split("-");

    const date = new Date(
      Date.UTC(Number(year), Number(month) - 1, Number(day))
    );

    const hebrewDate = formatHebrewDate(date);

    row.children[1].append(
      textElement("time", "", `${day}/${month}/${year}`),
      textElement("span", "item-description", hebrewDate)
    );

    const names = event.invitedUserIds.map(userId => {
      const member = state.assignees.find(
        member => member.id === userId
      );

      return member ? member.username : `משתמש ${userId}`;
    });

    row.children[2].append(
      textElement("span", "", names.join(", ") || "אין מוזמנים")
    );

    const actions = row.querySelector(".row-actions");

    actions.append(
      actionButton(
        "edit",
        `עריכת האירוע ${event.description}`,
        () => openEvent(event)
      )
    );

    actions.append(
      actionButton(
        "trash",
        `מחיקת האירוע ${event.description}`,
        () => confirmDelete(
          "למחוק את האירוע?",
          `האירוע ״${event.description}״ יוסר מהלוח המשפחתי.`,
          async () => {
            await api(`/events/${event.id}`, "DELETE");
            notify("האירוע נמחק.");
          }
        ),
        true
      )
    );

    body.append(row);
  });

  $("#boardCount").textContent =
    `${events.length} אירועים מוצגים · ${state.events.length} בסך הכול`;

  showTableOrEmpty(events.length, Boolean(query));
}

function showTableOrEmpty(count, filtered) {
  $("#tableContainer").hidden = count === 0;
  $("#emptyState").hidden = count > 0;

  const shopping = state.view === "shopping";
  const events = state.view === "events";
  const canCreate = shopping || events || isAdmin();

  const glyph = events ? "clock" : shopping ? "shopping" : "tasks";

  $("#emptyIcon").setAttribute(
    "href",
    `/assets/icons.svg#${glyph}`
  );

  $("#emptyTitle").textContent = filtered
    ? "לא נמצאו תוצאות מתאימות"
    : events
      ? "עוד אין אירועים בלוח"
      : shopping
        ? "יש מקום למה שחסר"
        : "מתחילים בלוח נקי";

  $("#emptyDescription").textContent = filtered
    ? "נסו חיפוש אחר, או נקו את הסינון."
    : events
      ? "מתכננים מפגש? הוסיפו אירוע ובחרו את המוזמנים."
      : shopping
        ? "חסר משהו בבית? הוסיפו אותו לרשימה."
        : isAdmin()
          ? "הוסיפו משימה ובחרו מי אחראי עליה."
          : "כשהמנהל יוסיף משימות, הן יופיעו כאן.";

  const button = $("#emptyAction");

  button.hidden = !filtered && !canCreate;

  button.textContent = filtered
    ? "ניקוי הסינון"
    : events
      ? "הוספת אירוע ראשון"
      : shopping
        ? "הוספת מוצר ראשון"
        : "הוספת משימה ראשונה";

  button.onclick = () => {
    if (filtered) {
      state.filter = "all";
      $("#searchInput").value = "";
      $("#dayFilter").value = "all";
      render();
      return;
    }

    if (events) {
      openEvent();
    } else if (shopping) {
      openShopping();
    } else {
      openTask();
    }
  };
}

function resetForm(form) {
  form.reset();
  const error = form.querySelector(".form-error");
  if (error) error.hidden = true;
}

function openTask(task) {
  if (!isAdmin()) return;
  const form = $("#taskForm");
  resetForm(form);
  form.elements.namedItem("id").value = task?.id || "";
  $("#taskDialogTitle").textContent = task ? "עריכת המשימה" : "משימה חדשה";
  $("#taskName").value = task?.name || "";
  $("#taskDescription").value = task?.description || "";
  $("#taskDay").querySelectorAll("[data-custom-day]").forEach(option => option.remove());
  const todayName = new Intl.DateTimeFormat("en-US", { timeZone: "Asia/Jerusalem", weekday: "long" }).format(new Date());
  const selectedDay = task ? dayIndex(task.day) : dayValues.indexOf(todayName);
  if (task && selectedDay < 0) {
    const option = new Option(task.day, task.day);
    option.dataset.customDay = "true";
    $("#taskDay").append(option);
    $("#taskDay").value = task.day;
  } else $("#taskDay").value = dayValues[selectedDay];
  const select = $("#taskAssignee");
  select.replaceChildren(new Option("בחירת אחראי", ""));
  state.assignees.forEach(member => select.append(new Option(member.username, member.id)));
  const selected = state.assignees.find(member => member.username === task?.assigneeUsername);
  select.value = selected ? String(selected.id) : "";
  $("#taskDialog").showModal();
  $("#taskName").focus();
}

function openShopping(item) {
  const form = $("#shoppingForm");
  resetForm(form);
  form.elements.namedItem("id").value = item?.id || "";
  $("#shoppingDialogTitle").textContent = item ? "עדכון מוצר" : "מה חסר בבית?";
  $("#productName").value = item?.productName || "";
  $("#productQuantity").value = item?.quantity || 1;
  $("#shoppingDialog").showModal();
  $("#productName").focus();
}

function openEvent(event) {
  const form = $("#eventForm");
  resetForm(form);

  form.elements.namedItem("id").value = event?.id || "";
  $("#eventDialogTitle").textContent =
    event ? "עריכת אירוע" : "אירוע חדש";

  $("#eventDescription").value = event?.description || "";
  $("#eventDate").value = event?.eventDate || "";

  const invitees = $("#eventInvitees");
  invitees.replaceChildren();

  state.assignees.forEach(member => {
    const label = document.createElement("label");
    label.className = "invitee-option";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.name = "invitedUserIds";
    checkbox.value = member.id;
    checkbox.checked =
      event?.invitedUserIds.includes(member.id) || false;

    label.append(
      checkbox,
      textElement("span", "", member.username)
    );

    invitees.append(label);
  });

  $("#eventDialog").showModal();
  $("#eventDescription").focus();
}

function renderMembers() {
  const list = $("#membersList");
  list.replaceChildren();
  state.assignees.forEach(member => {
    const chip = document.createElement("span");
    chip.className = "member-chip";
    chip.append(avatar(member.username), textElement("span", "", member.username));
    const label = document.createElement("label"); label.className = "driver-option";
    const checkbox = document.createElement("input"); checkbox.type = "checkbox"; checkbox.checked = member.driver;
    checkbox.setAttribute("aria-label", "נהג: " + member.username);
    checkbox.addEventListener("change", () => runButtonAction(checkbox, async () => {
      try { await api("/members/" + member.id + "/driver", "PATCH", { driver: checkbox.checked }); }
      finally { await loadData(); renderMembers(); }
    }));
    label.append(checkbox, "נהג / נהגת"); chip.append(label);
    if (member.id !== state.user.id) chip.append(actionButton("trash", "מחיקת המשתמש " + member.username, () => confirmDelete(
      "למחוק את המשתמש?", "החשבון של " + member.username + " יימחק. הזמנות הרכב שלו יבוטלו והמשימות יישארו ללא אחראי. לא ניתן לבטל את המחיקה.", async () => {
        await api("/members/" + member.id, "DELETE");
        await loadData(); renderMembers(); notify("המשתמש נמחק.");
      }
    ), true));
    list.append(chip);
  });
}

function confirmDelete(title, description, action) {
  resetForm($("#confirmForm"));
  $("#confirmTitle").textContent = title;
  $("#confirmDescription").textContent = description;
  state.pendingDelete = action;
  $("#confirmDialog").showModal();
}

async function runButtonAction(button, action) {
  button.disabled = true;
  try { await action(); } catch (error) { notify(error.message, true); }
  finally { button.disabled = false; }
}

function handleForm(form, action) {
  form.addEventListener("submit", async event => {
    event.preventDefault();
    if (form.dataset.busy === "true" || !form.reportValidity()) return;
    const errorArea = form.querySelector(".form-error");
    errorArea.hidden = true;
    form.dataset.busy = "true";
    form.querySelectorAll("button").forEach(button => { button.disabled = true; });
    try { await action(form); }
    catch (error) {
      errorArea.textContent = error.message || "לא הצלחנו לשמור. נסו שוב.";
      errorArea.hidden = false;
    } finally {
      form.dataset.busy = "false";
      form.querySelectorAll("button").forEach(button => { button.disabled = false; });
    }
  });
}

handleForm($("#taskForm"), async form => {
  const id = form.elements.namedItem("id").value;
  const body = {
    name: $("#taskName").value.trim(), day: $("#taskDay").value,
    description: $("#taskDescription").value.trim(),
    assigneeId: Number($("#taskAssignee").value)
  };
  if (!body.name) throw new Error("צריך למלא שם למשימה.");
  await api(id ? `/tasks/${id}` : "/tasks", id ? "PUT" : "POST", body);
  $("#taskDialog").close();
  notify(id ? "המשימה עודכנה." : "המשימה נוספה ללוח המשפחתי.");
  await loadData();
});

handleForm($("#shoppingForm"), async form => {
  const id = form.elements.namedItem("id").value;
  const body = { productName: $("#productName").value.trim(), quantity: Number($("#productQuantity").value) };
  if (!body.productName) throw new Error("צריך למלא שם מוצר.");
  if (!Number.isInteger(body.quantity) || body.quantity < 1) throw new Error("הכמות חייבת להיות מספר שלם גדול מאפס.");
  await api(id ? `/shopping-items/${id}` : "/shopping-items", id ? "PUT" : "POST", body);
  $("#shoppingDialog").close();
  notify(id ? "המוצר עודכן, יחד עם תאריך העדכון." : "המוצר נוסף לרשימת הקניות.");
  await loadData();
});

handleForm($("#eventForm"), async form => {
  const id = form.elements.namedItem("id").value;

  const invitedUserIds = Array.from(
    form.querySelectorAll(
      'input[name="invitedUserIds"]:checked'
    ),
    checkbox => Number(checkbox.value)
  );

  const body = {
    description: $("#eventDescription").value.trim(),
    eventDate: $("#eventDate").value,
    invitedUserIds
  };

  if (!body.description) {
    throw new Error("צריך למלא תיאור לאירוע.");
  }

  if (!body.eventDate) {
    throw new Error("צריך לבחור תאריך לאירוע.");
  }

  await api(
    id ? `/events/${id}` : "/events",
    id ? "PUT" : "POST",
    body
  );

  $("#eventDialog").close();
  notify(id ? "האירוע עודכן." : "האירוע נוסף ללוח.");

  await loadData();
});

$("#openPassword").addEventListener("click", () => {
  resetForm($("#passwordForm")); $("#passwordDialog").showModal(); $("#currentPassword").focus();
});
handleForm($("#passwordForm"), async () => {
  const newPassword = $("#newPassword").value;
  if (newPassword !== $("#confirmPassword").value) throw new Error("הסיסמאות החדשות אינן תואמות.");
  if (new TextEncoder().encode(newPassword).length > 72) throw new Error("הסיסמה ארוכה מדי. נסו סיסמה קצרה יותר.");
  await api("/me/password", "PATCH", { currentPassword: $("#currentPassword").value, newPassword });
  $("#passwordDialog").close();
  location.assign("/login.html?passwordChanged");
});

handleForm($("#memberForm"), async form => {
  const username = $("#memberUsername").value.trim();
  const password = $("#memberPassword").value;
  if (new TextEncoder().encode(password).length > 72) throw new Error("הסיסמה ארוכה מדי. נסו סיסמה קצרה יותר.");
  await api("/members", "POST", { username, password, driver: $("#memberDriver").checked });
  form.reset();
  await loadData();
  renderMembers();
  notify(`החשבון של ${username} נוצר. אפשר להתחבר עם הפרטים שבחרתם.`);
});

handleForm($("#confirmForm"), async () => {
  const action = state.pendingDelete;
  if (!action) return;
  await action();
  $("#confirmDialog").close();
  await loadData();
});

document.querySelectorAll("dialog").forEach(dialog => {
  dialog.querySelectorAll("[data-close]").forEach(button => button.addEventListener("click", () => dialog.close()));
  dialog.addEventListener("cancel", event => {
    if (dialog.querySelector("form")?.dataset.busy === "true") event.preventDefault();
  });
  dialog.addEventListener("close", () => {
    resetForm(dialog.querySelector("form"));
    if (dialog.id === "confirmDialog") state.pendingDelete = null;
  });
});

document.querySelectorAll("[data-view]").forEach(button => button.addEventListener("click", () => setView(button.dataset.view)));
document.querySelectorAll("[data-filter]").forEach(button => button.addEventListener("click", () => {
  state.filter = button.dataset.filter;
  render();
}));
$("#searchInput").addEventListener("input", render);
$("#dayFilter").addEventListener("change", render);
$("#refreshButton").addEventListener("click", () => loadData());
$("#primaryAction").addEventListener("click", () => {
  if (state.view === "vehicles") {
    if (isAdmin() && state.user.driver) { resetForm($("#vehicleForm")); $("#vehicleDialog").showModal(); }
  } else if (state.view === "events") {
    openEvent();
  } else if (state.view === "shopping") {
    openShopping();
  } else {
    openTask();
  }
});
$("#openMembers").addEventListener("click", () => {
  if (!isAdmin()) return;
  resetForm($("#memberForm"));
  renderMembers();
  $("#memberDialog").showModal();
  $("#memberUsername").focus();
});
$("#logoutButton").addEventListener("click", event => runButtonAction(event.currentTarget, async () => {
  const csrf = await getCsrf();
  const form = document.createElement("form");
  form.method = "post";
  form.action = "/logout";
  form.hidden = true;
  const input = document.createElement("input");
  input.name = csrf.parameterName;
  input.value = csrf.token;
  form.append(input);
  document.body.append(form);
  form.submit();
}));

const now = new Date();

const gregorianToday = new Intl.DateTimeFormat("he-IL", {
  timeZone: "Asia/Jerusalem",
  weekday: "long",
  day: "numeric",
  month: "long"
}).format(now);

const israelParts = Object.fromEntries(
  new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Jerusalem",
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(now).map(part => [part.type, part.value])
);

const todayDate = new Date(Date.UTC(
  Number(israelParts.year),
  Number(israelParts.month) - 1,
  Number(israelParts.day)
));

$("#today").textContent =
  `${gregorianToday} · ${formatHebrewDate(todayDate)}`;
render();
loadData(true);

function renderVehicles() {
  document.title = "מי משתמש ברכב · ביחד בבית";
  document.querySelectorAll("[data-view]").forEach(b => {
    b.classList.toggle("active", b.dataset.view === "vehicles");
    if (b.dataset.view === "vehicles") b.setAttribute("aria-current", "page"); else b.removeAttribute("aria-current");
  });
  $("#pageTitle").textContent = "מי משתמש ברכב";
  $("#pageEyebrow").textContent = "מתכננים את הנסיעות ביחד";
  $("#pageDescription").textContent = "בחרו רכב וחודש ותפסו משבצת פנויה על שמכם.";
  $("#boardTitle").textContent = "לוח שימוש חודשי";
  $("#primaryAction").hidden = !isAdmin();
  $("#primaryAction span").textContent = "הוספת רכב";
  $("#tableContainer").hidden = true; $("#emptyState").hidden = true;
  $("#boardNote").textContent = "בוקר · צהריים · ערב. רק המזמין או מנהל יכולים לבטל הזמנה.";
  $("#vehicleMonth").value = state.vehicleMonth;
  const select = $("#vehicleSelect"); select.replaceChildren();
  state.vehicles.forEach(v => select.append(new Option(v.name, v.id)));
  if (!state.vehicles.some(v => String(v.id) === String(state.vehicleId))) state.vehicleId = String(state.vehicles[0]?.id || "");
  select.value = state.vehicleId;
  $("#vehicleEmpty").hidden = state.vehicles.length > 0;
  const calendar = $("#vehicleCalendar"); calendar.replaceChildren();
  const bookings = state.bookings.filter(b => String(b.vehicleId) === state.vehicleId);
  $("#boardCount").textContent = bookings.length + " משבצות מוזמנות בחודש הנבחר";
  if (!state.vehicleId) return;
  dayNames.forEach(day => calendar.append(textElement("div", "calendar-weekday", day)));
  const [year, month] = state.vehicleMonth.split("-").map(Number);
  const first = new Date(state.vehicleMonth + "-01T12:00:00Z");
  const end = new Date(first); end.setUTCMonth(end.getUTCMonth() + 1); end.setUTCDate(0);
  for (let i = 0; i < first.getUTCDay(); i++) calendar.append(textElement("div", "calendar-spacer", ""));
  const today = new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Jerusalem" }).format(new Date());
  for (let day = 1; day <= end.getUTCDate(); day++) {
    const date = state.vehicleMonth + "-" + String(day).padStart(2, "0");
    const cell = textElement("article", "calendar-day", "");
    const weekday = dayNames[(first.getUTCDay() + day - 1) % 7];
    cell.append(textElement("h3", "", `${day} · יום ${weekday}`));
    for (const [slot, label] of [["MORNING", "בוקר"], ["AFTERNOON", "צהריים"], ["EVENING", "ערב"]]) {
      const booking = bookings.find(b => b.date === date && b.slot === slot);
      const button = textElement("button", "vehicle-slot" + (booking ? " booked" : ""), label + " · " + (booking ? booking.username : "פנוי"));
      button.type = "button";
      const canCancel = booking && (booking.userId === state.user.id || isAdmin());
      button.disabled = booking ? !canCancel : date < today;
      button.setAttribute("aria-label", date + " " + label + " " + (booking ? booking.username + (canCancel ? " — ביטול הזמנה" : "") : "הזמנת רכב"));
      button.addEventListener("click", () => {
        if (booking) confirmDelete("לבטל את ההזמנה?", date + " · " + label, async () => {
          await api("/vehicles/bookings/" + booking.id, "DELETE"); notify("ההזמנה בוטלה.");
        });
        else runButtonAction(button, async () => {
          try { await api("/vehicles/bookings", "POST", { vehicleId: Number(state.vehicleId), date, slot }); notify("הרכב הוזמן על שמך."); }
          finally { await loadData(); }
        });
      });
      cell.append(button);
    }
    calendar.append(cell);
  }
}
$("#vehicleSelect").addEventListener("change", event => { state.vehicleId = event.target.value; render(); });
$("#vehicleMonth").addEventListener("change", async event => {
  if (state.loading) {
    event.target.value = state.vehicleMonth;
    return;
  }

  if (
    !event.target.validity.valid ||
    !/^\d{4}-\d{2}$/.test(event.target.value)
  ) {
    return;
  }

  state.vehicleMonth = event.target.value;
  await loadData();
});
handleForm($("#vehicleForm"), async () => {
  await api("/vehicles", "POST", { name: $("#vehicleName").value.trim() });
  $("#vehicleDialog").close(); await loadData(); notify("הרכב נוסף.");
});
