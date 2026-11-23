import { api } from "../api.js";
import {
  formatHebrewDateFromIso,
  formatIsoAsDisplayDate,
  israelToday
} from "../dates.js";
import {
  $,
  $$,
  actionButton,
  confirmAction,
  createTableRow,
  handleForm,
  notify,
  resetForm,
  setTableHeaders,
  textElement
} from "../dom.js";
import { state } from "../state.js";

export function filterAndSortEvents(events, query = "") {
  const normalized = query.trim().toLowerCase();
  return events
    .filter(
      event =>
        !normalized ||
        String(event.description || "").toLowerCase().includes(normalized)
    )
    .sort(
      (a, b) =>
        String(a.eventDate || "").localeCompare(String(b.eventDate || "")) ||
        a.id - b.id
    );
}

export function resolveInviteeNames(
  invitedUserIds,
  assignees = state.assignees
) {
  if (!Array.isArray(invitedUserIds)) return [];
  return invitedUserIds.map(userId => {
    const member = assignees.find(person => person.id === userId);
    return member ? member.username : `משתמש ${userId}`;
  });
}

export function renderEvents({ onReload, onShowEmpty }) {
  const query = $("#searchInput").value;
  const events = filterAndSortEvents(state.events, query);
  const today = israelToday();

  setTableHeaders([
    "האירוע",
    "תאריך",
    "מוזמנים",
    { srOnly: "פעולות" }
  ]);

  const body = $("#tableBody");
  body.replaceChildren();

  events.forEach(event => {
    const row = createTableRow([
      { className: "item-main" },
      { label: "תאריך" },
      { label: "מוזמנים" },
      { actions: true }
    ]);

    const isPast = Boolean(event.eventDate && event.eventDate < today);
    const isToday = event.eventDate === today;
    row.classList.toggle("event-past-row", isPast);

    row.children[0].append(
      textElement("span", "item-title", event.description)
    );

    const time = textElement(
      "time",
      "",
      formatIsoAsDisplayDate(event.eventDate)
    );
    if (event.eventDate) time.dateTime = event.eventDate;
    row.children[1].append(
      time,
      textElement(
        "span",
        "item-description",
        formatHebrewDateFromIso(event.eventDate)
      )
    );
    if (isToday) {
      row.children[1].append(textElement("span", "event-today", "היום"));
    } else if (isPast) {
      row.children[1].append(textElement("span", "event-past", "עבר"));
    }

    const names = resolveInviteeNames(event.invitedUserIds, state.assignees);
    if (names.length) {
      const tags = textElement("div", "invitee-tags", "");
      names.forEach(name => tags.append(textElement("span", "invitee-tag", name)));
      row.children[2].append(tags);
    } else {
      row.children[2].append(textElement("span", "muted", "אין מוזמנים"));
    }

    const actions = row.querySelector(".row-actions");
    actions.append(
      actionButton("edit", `עריכת האירוע ${event.description}`, () =>
        openEvent(event)
      ),
      actionButton(
        "trash",
        `מחיקת האירוע ${event.description}`,
        () =>
          confirmAction(
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
  onShowEmpty(events.length, Boolean(query.trim()));
}

export function openEvent(event) {
  const form = $("#eventForm");
  resetForm(form);

  form.elements.namedItem("id").value = event?.id || "";
  $("#eventDialogTitle").textContent = event ? "עריכת אירוע" : "אירוע חדש";
  $("#eventDescription").value = event?.description || "";
  $("#eventDate").value = event?.eventDate || "";

  const invitedIds = Array.isArray(event?.invitedUserIds)
    ? event.invitedUserIds
    : [];
  const invitees = $("#eventInvitees");
  invitees.replaceChildren();

  state.assignees.forEach(member => {
    const label = document.createElement("label");
    label.className = "invitee-option";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.name = "invitedUserIds";
    checkbox.value = member.id;
    checkbox.checked = invitedIds.includes(member.id);

    label.append(checkbox, textElement("span", "", member.username));
    invitees.append(label);
  });

  $("#eventDialog").showModal();
  $("#eventDescription").focus();
}

export function initEvents({ onReload }) {
  $("#selectAllInvitees")?.addEventListener("click", () => {
    $$('#eventInvitees input[name="invitedUserIds"]').forEach(box => {
      box.checked = true;
    });
  });

  $("#clearAllInvitees")?.addEventListener("click", () => {
    $$('#eventInvitees input[name="invitedUserIds"]').forEach(box => {
      box.checked = false;
    });
  });

  handleForm($("#eventForm"), async form => {
    const id = form.elements.namedItem("id").value;
    const invitedUserIds = $$(
      'input[name="invitedUserIds"]:checked',
      form
    ).map(checkbox => Number(checkbox.value));

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

    await api(id ? `/events/${id}` : "/events", id ? "PUT" : "POST", body);
    $("#eventDialog").close();
    notify(id ? "האירוע עודכן." : "האירוע נוסף ללוח.");
    await onReload();
  });
}
