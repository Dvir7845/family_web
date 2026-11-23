import { api, getCsrf, unwrapList } from "./js/api.js";
import { formatHeaderToday, formatSyncTime } from "./js/dates.js";
import {
  $,
  $$,
  avatar,
  clearPageError,
  handleForm,
  runButtonAction,
  showPageError,
  wireDialogs
} from "./js/dom.js";
import { renderStats, renderViewChrome } from "./js/stats.js";
import {
  canManageTasks,
  canManageVehicles,
  canUseVehicles,
  isAdmin,
  parseHashView,
  state
} from "./js/state.js";
import { initEvents, openEvent, renderEvents } from "./js/views/events.js";
import { initMembers, renderMembers } from "./js/views/members.js";
import { initShopping, openShopping, renderShopping } from "./js/views/shopping.js";
import {
  initTasks,
  openTask,
  renderTasks,
  renderWeeklySchedule,
  renderWeeklyTasks
} from "./js/views/tasks.js";
import {
  initVehicles,
  openVehicleDialog,
  renderVehicles
} from "./js/views/vehicles.js";

async function loadData(initial = false) {
  if (state.loading) return;
  state.loading = true;
  $("#refreshButton").disabled = true;
  $("#vehicleMonth").disabled = true;
  clearPageError();

  if (initial || !state.loaded) {
    $("#loadingState").hidden = false;
    $("#emptyState").hidden = true;
    $("#tableContainer").hidden = true;
  }

  try {
    const user = await api("/me");
    const [tasks, shopping, assignees, events, weeklyStats] = await Promise.all([
      api("/tasks"),
      api("/shopping-items"),
      api("/members"),
      api("/events"),
      api("/tasks/statistics/weekly")
    ]);

    state.user = user;
    $("#vehicleNavigation").hidden = !canUseVehicles(user);
    if (!canUseVehicles(user) && state.view === "vehicles") {
      state.view = "tasks";
    }

    if (canUseVehicles(user)) {
      const [vehicles, bookings] = await Promise.all([
        api("/vehicles"),
        api(`/vehicles/bookings?month=${state.vehicleMonth}`)
      ]);
      state.vehicles = unwrapList(vehicles);
      state.bookings = unwrapList(bookings);
    } else {
      state.vehicles = [];
      state.bookings = [];
    }

    state.weeklyStats = weeklyStats;
    state.tasks = unwrapList(tasks);
    state.shopping = unwrapList(shopping);
    state.assignees = unwrapList(assignees);
    state.events = unwrapList(events);
    state.loaded = true;

    $("#currentUsername").textContent = user.username;
    $("#currentRole").textContent = isAdmin(user)
      ? "מנהל"
      : canManageTasks(user)
        ? "אחראי משימות"
        : "בן / בת משפחה";
    $("#userAvatar").replaceWith(
      Object.assign(avatar(user.username), { id: "userAvatar" })
    );
    $("#adminNavigation").hidden = !isAdmin(user);
    $("#navTaskCount").textContent = state.tasks.filter(
      task => !task.completed
    ).length;
    $("#navTaskCount").title = "משימות פתוחות";
    $("#navShoppingCount").textContent = state.shopping.length;
    $("#navEventCount").textContent = state.events.length;
    $("#lastSynced").textContent = formatSyncTime();

    render();
    if ($("#memberDialog")?.open) {
      renderMembers({ onReload: loadData });
    }
  } catch (error) {
    showPageError(error);
  } finally {
    state.loading = false;
    $("#loadingState").hidden = true;
    $("#refreshButton").disabled = false;
    $("#vehicleMonth").disabled = false;
  }
}

function updateClearFiltersButton() {
  const button = $("#clearFiltersButton");
  if (!button) return;
  const hasQuery = Boolean($("#searchInput").value.trim());
  const hasTaskFilters =
    state.view === "tasks" &&
    ($("#dayFilter").value !== "all" ||
      $("#taskTypeFilter").value !== "all" ||
      state.filter !== "all");
  button.hidden = state.view === "vehicles" || (!hasQuery && !hasTaskFilters);
}

function clearFilters() {
  state.filter = "all";
  $("#searchInput").value = "";
  $("#dayFilter").value = "all";
  $("#taskTypeFilter").value = "all";
  render();
}

function showTableOrEmpty(count, filtered) {
  $("#tableContainer").hidden = count === 0;
  $("#emptyState").hidden = count > 0;

  const shopping = state.view === "shopping";
  const events = state.view === "events";
  const canCreate = shopping || events || canManageTasks();
  const glyph = events ? "calendar" : shopping ? "shopping" : "tasks";

  $("#emptyIcon").setAttribute("href", `/assets/icons.svg#${glyph}`);
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
        : canManageTasks()
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
      clearFilters();
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

function render() {
  const showWeekly = state.view === "tasks" && state.loaded;
  $("#weeklyTaskPanel").hidden = !showWeekly;
  $("#weeklySchedulePanel").hidden = !showWeekly;
  if (showWeekly) {
    renderWeeklyTasks();
    renderWeeklySchedule({ onReload: loadData });
  }

  const vehicles = state.view === "vehicles" && canUseVehicles();
  $("#vehicleBoard").hidden = !vehicles;
  $(".board-toolbar").hidden = vehicles;
  $(".stats-grid").hidden = vehicles;

  if (vehicles) {
    renderVehicles({ onReload: loadData });
    return;
  }

  renderViewChrome(state);
  updateClearFiltersButton();
  if (!state.loaded) return;

  renderStats(state);
  if (state.view === "events") {
    renderEvents({ onReload: loadData, onShowEmpty: showTableOrEmpty });
  } else if (state.view === "shopping") {
    renderShopping({ onReload: loadData, onShowEmpty: showTableOrEmpty });
  } else {
    renderTasks({ onReload: loadData, onShowEmpty: showTableOrEmpty });
  }
}

function setView(view, { updateHash = true } = {}) {
  if (view === "vehicles" && !canUseVehicles()) return;
  if (state.view !== view) {
    state.filter = "all";
    $("#searchInput").value = "";
    $("#dayFilter").value = "all";
    $("#taskTypeFilter").value = "all";
  }
  state.view = view;
  if (updateHash) {
    history.replaceState(null, "", `#${view}`);
  }
  render();
}

wireDialogs();

handleForm($("#confirmForm"), async () => {
  const action = state.pendingDelete;
  if (!action) return;
  await action();
  $("#confirmDialog").close();
  await loadData();
});

$$("[data-view]").forEach(button =>
  button.addEventListener("click", () => setView(button.dataset.view))
);
$$("[data-filter]").forEach(button =>
  button.addEventListener("click", () => {
    state.filter = button.dataset.filter;
    render();
  })
);
$("#searchInput").addEventListener("input", render);
$("#dayFilter").addEventListener("change", render);
$("#taskTypeFilter").addEventListener("change", render);
$("#clearFiltersButton")?.addEventListener("click", clearFilters);
$("#refreshButton").addEventListener("click", () => loadData());
$("#primaryAction").addEventListener("click", () => {
  if (state.view === "vehicles") {
    if (canManageVehicles()) openVehicleDialog();
  } else if (state.view === "events") {
    openEvent();
  } else if (state.view === "shopping") {
    openShopping();
  } else {
    openTask();
  }
});
$("#logoutButton").addEventListener("click", event =>
  runButtonAction(event.currentTarget, async () => {
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
  })
);
window.addEventListener("hashchange", () =>
  setView(parseHashView(location.hash), { updateHash: false })
);

initTasks({ onReload: loadData });
initShopping({ onReload: loadData });
initEvents({ onReload: loadData });
initVehicles({ onReload: loadData, onRender: render });
initMembers({ onReload: loadData });

$("#today").textContent = formatHeaderToday();
render();
loadData(true);
