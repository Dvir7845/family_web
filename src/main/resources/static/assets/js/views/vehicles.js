import { api } from "../api.js";
import {
  DAY_NAMES,
  currentIsraelMonth,
  israelToday,
  shiftYearMonth
} from "../dates.js";
import {
  $,
  confirmAction,
  handleForm,
  notify,
  resetForm,
  runButtonAction,
  setBoardNote,
  textElement
} from "../dom.js";
import { canManageVehicles, isAdmin, state } from "../state.js";
import { VIEW_META, syncNavigation } from "../stats.js";

const SLOTS = [
  ["MORNING", "בוקר"],
  ["AFTERNOON", "צהריים"],
  ["EVENING", "ערב"]
];

export function renderVehicles({ onReload } = {}) {
  const meta = VIEW_META.vehicles;
  document.title = `${meta.title} · ביחד בבית`;
  syncNavigation("vehicles");

  $("#pageTitle").textContent = meta.title;
  $("#pageEyebrow").textContent = meta.eyebrow;
  $("#pageDescription").textContent = meta.description;
  $("#boardTitle").textContent = meta.boardTitle;
  $("#primaryAction").hidden = !canManageVehicles();
  $("#primaryAction span").textContent = meta.actionLabel;
  $("#tableContainer").hidden = true;
  $("#emptyState").hidden = true;
  setBoardNote(meta.noteIcon, meta.noteText);

  $("#vehicleMonth").value = state.vehicleMonth;

  const select = $("#vehicleSelect");
  select.replaceChildren();
  state.vehicles.forEach(vehicle =>
    select.append(new Option(vehicle.name, vehicle.id))
  );
  if (!state.vehicles.some(v => String(v.id) === String(state.vehicleId))) {
    state.vehicleId = String(state.vehicles[0]?.id || "");
  }
  select.value = state.vehicleId;

  $("#deleteVehicle").hidden = !canManageVehicles() || !state.vehicleId;
  $("#vehicleEmpty").hidden = state.vehicles.length > 0;

  const calendar = $("#vehicleCalendar");
  calendar.replaceChildren();

  const bookings = state.bookings.filter(
    booking => String(booking.vehicleId) === state.vehicleId
  );
  $("#boardCount").textContent = `${bookings.length} משבצות מוזמנות בחודש הנבחר`;
  if (!state.vehicleId) return;

  DAY_NAMES.forEach(day =>
    calendar.append(textElement("div", "calendar-weekday", day))
  );

  const first = new Date(`${state.vehicleMonth}-01T12:00:00Z`);
  const end = new Date(first);
  end.setUTCMonth(end.getUTCMonth() + 1);
  end.setUTCDate(0);

  for (let i = 0; i < first.getUTCDay(); i++) {
    calendar.append(textElement("div", "calendar-spacer", ""));
  }

  const today = israelToday();
  for (let day = 1; day <= end.getUTCDate(); day++) {
    const date = `${state.vehicleMonth}-${String(day).padStart(2, "0")}`;
    const cell = textElement(
      "article",
      `calendar-day${date === today ? " today" : ""}`,
      ""
    );
    const weekday = DAY_NAMES[(first.getUTCDay() + day - 1) % 7];
    cell.append(textElement("h3", "", `${day} · יום ${weekday}`));

    for (const [slot, label] of SLOTS) {
      const booking = bookings.find(b => b.date === date && b.slot === slot);
      const isMine = Boolean(booking && booking.userId === state.user?.id);
      const slotClass = booking
        ? `vehicle-slot booked${isMine ? " mine" : ""}`
        : "vehicle-slot";
      const button = textElement(
        "button",
        slotClass,
        `${label} · ${booking ? booking.username : "פנוי"}`
      );
      button.type = "button";

      const canCancel = Boolean(booking && (isMine || isAdmin()));
      button.disabled = booking ? !canCancel : date < today;
      button.setAttribute(
        "aria-label",
        `${date} ${label} ${
          booking
            ? booking.username + (canCancel ? " — ביטול הזמנה" : "")
            : "הזמנת רכב"
        }`
      );
      button.addEventListener("click", () => {
        if (booking) {
          confirmAction(
            "לבטל את ההזמנה?",
            `${date} · ${label}`,
            async () => {
              await api(`/vehicles/bookings/${booking.id}`, "DELETE");
              notify("ההזמנה בוטלה.");
            }
          );
        } else {
          runButtonAction(button, async () => {
            try {
              await api("/vehicles/bookings", "POST", {
                vehicleId: Number(state.vehicleId),
                date,
                slot
              });
              notify("הרכב הוזמן על שמך.");
            } finally {
              if (onReload) await onReload();
            }
          });
        }
      });
      cell.append(button);
    }

    calendar.append(cell);
  }
}

export function openVehicleDialog() {
  resetForm($("#vehicleForm"));
  $("#vehicleDialog").showModal();
  $("#vehicleName").focus();
}

export function initVehicles({ onReload, onRender }) {
  const changeMonth = async nextMonth => {
    if (state.loading || !nextMonth || nextMonth === state.vehicleMonth) return;
    state.vehicleMonth = nextMonth;
    await onReload();
  };

  $("#deleteVehicle").addEventListener("click", () => {
    if (!canManageVehicles()) return;
    const vehicle = state.vehicles.find(
      v => String(v.id) === String(state.vehicleId)
    );
    if (!vehicle) return;
    confirmAction(
      "למחוק את הרכב?",
      `הרכב ״${vehicle.name}״ וכל ההזמנות שלו יימחקו. לא ניתן לבטל את המחיקה.`,
      async () => {
        await api(`/vehicles/${vehicle.id}`, "DELETE");
        notify("הרכב וההזמנות שלו נמחקו.");
      }
    );
  });

  $("#vehicleSelect").addEventListener("change", event => {
    state.vehicleId = event.target.value;
    onRender();
  });

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
    await changeMonth(event.target.value);
  });

  $("#vehiclePrevMonth")?.addEventListener("click", () =>
    changeMonth(shiftYearMonth(state.vehicleMonth, -1))
  );
  $("#vehicleNextMonth")?.addEventListener("click", () =>
    changeMonth(shiftYearMonth(state.vehicleMonth, 1))
  );
  $("#vehicleTodayMonth")?.addEventListener("click", () =>
    changeMonth(currentIsraelMonth())
  );

  handleForm($("#vehicleForm"), async () => {
    const name = $("#vehicleName").value.trim();
    if (!name) throw new Error("צריך למלא שם לרכב.");
    await api("/vehicles", "POST", { name });
    $("#vehicleDialog").close();
    await onReload();
    notify("הרכב נוסף.");
  });
}
