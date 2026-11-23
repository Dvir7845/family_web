import { api } from "../api.js";
import {
  DAY_VALUES,
  dayIndex,
  dayLabel,
  formatHebrewDateFromIso,
  formatIsoAsDisplayDate,
  formatShortUtcDate,
  getWeekDays,
  israelToday
} from "../dates.js";
import {
  $,
  $$,
  actionButton,
  avatar,
  confirmAction,
  createIcon,
  createTableRow,
  handleForm,
  notify,
  resetForm,
  runButtonAction,
  setTableHeaders,
  textElement
} from "../dom.js";
import { canManageTasks, state } from "../state.js";

export function taskType(task) {
  const type = task?.type || task?.taskType;
  if (type === "ONETIME" || type === "ONE_TIME") return "ONETIME";
  if (type === "WEEKLY") return "WEEKLY";
  if (task?.taskDate) return "ONETIME";
  return task?.day ? "WEEKLY" : "";
}

export function taskAssignee(task, assignees = state.assignees) {
  const id = task?.assigneeId ?? task?.owner?.id ?? null;
  const username = task?.assigneeUsername || task?.owner?.username || "";
  const member = assignees.find(
    person =>
      (id !== null && String(person.id) === String(id)) ||
      (username && person.username === username)
  );
  return {
    id: id ?? member?.id ?? null,
    username: username || member?.username || ""
  };
}

export function isMyTask(task, user = state.user, assignees = state.assignees) {
  if (!user) return false;
  const assignee = taskAssignee(task, assignees);
  if (assignee.id !== null) {
    return String(assignee.id) === String(user.id);
  }
  return Boolean(assignee.username && assignee.username === user.username);
}

export function taskDayIndex(task) {
  if (taskType(task) === "ONETIME") {
    if (!task?.taskDate) return -1;
    return new Date(`${task.taskDate}T00:00:00Z`).getUTCDay();
  }
  return dayIndex(task?.day);
}

export function taskSortDate(task, today = israelToday()) {
  if (taskType(task) === "ONETIME") {
    return task?.taskDate || "9999-12-31";
  }
  const day = taskDayIndex(task);
  if (day < 0) return "9999-12-31";
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + ((day - date.getUTCDay() + 7) % 7));
  return date.toISOString().slice(0, 10);
}

export function filterAndSortTasks(
  tasks,
  {
    query = "",
    day = "all",
    type = "all",
    filter = "all",
    today = israelToday(),
    user = state.user,
    assignees = state.assignees
  } = {}
) {
  const normalizedQuery = query.trim().toLowerCase();
  return tasks
    .filter(task => {
      const assignee = taskAssignee(task, assignees);
      const kindLabel = taskType(task) === "WEEKLY" ? "שבועית" : "חד־פעמית";
      const searchable = [
        task.name,
        task.description || "",
        assignee.username,
        task.taskDate || "",
        kindLabel
      ]
        .join(" ")
        .toLowerCase();

      return (
        (!normalizedQuery || searchable.includes(normalizedQuery)) &&
        (day === "all" || taskDayIndex(task) === Number(day)) &&
        (type === "all" || taskType(task) === type) &&
        (filter !== "mine" || isMyTask(task, user, assignees)) &&
        (filter !== "open" || !task.completed) &&
        (filter !== "done" || task.completed)
      );
    })
    .sort(
      (a, b) =>
        Number(a.completed) - Number(b.completed) ||
        taskSortDate(a, today).localeCompare(taskSortDate(b, today)) ||
        a.id - b.id
    );
}

export function syncTaskScheduleFields(
  isEdit = Boolean($("#taskForm")?.elements?.namedItem("id")?.value)
) {
  const weekly = $("#taskType").value === "WEEKLY";
  const dayField = $("#taskDayField");
  const selectWrap = $("#taskDaySelectWrap");
  const daysGroup = $("#taskDaysGroup");
  const daySelect = $("#taskDay");
  const dateInput = $("#taskDate");

  dayField.hidden = !weekly;
  dayField.classList.toggle("full-width", weekly && !isEdit);
  $("#taskDateField").hidden = weekly;

  dateInput.disabled = weekly;
  dateInput.required = !weekly;

  const useSingleSelect = weekly && isEdit;
  selectWrap.hidden = !useSingleSelect;
  daySelect.disabled = !useSingleSelect;
  daySelect.required = useSingleSelect;

  const useMultiDays = weekly && !isEdit;
  daysGroup.hidden = !useMultiDays;
  daysGroup.disabled = !useMultiDays;
  $$('input[name="days"]', daysGroup).forEach(checkbox => {
    checkbox.disabled = !useMultiDays;
  });
}

export function buildTaskPayload({
  id = "",
  name = "",
  description = "",
  assigneeId,
  type = "ONETIME",
  taskDate = null,
  singleDay = null,
  selectedDays = []
}) {
  const trimmedName = String(name || "").trim();
  const trimmedDescription = String(description || "").trim();
  const numericAssigneeId = Number(assigneeId);

  if (!["ONETIME", "WEEKLY"].includes(type)) {
    throw new Error("צריך לבחור סוג משימה.");
  }
  if (!trimmedName) {
    throw new Error("צריך למלא שם למשימה.");
  }
  if (!Number.isSafeInteger(numericAssigneeId) || numericAssigneeId <= 0) {
    throw new Error("צריך לבחור אחראי למשימה.");
  }

  if (type === "ONETIME") {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(String(taskDate || ""))) {
      throw new Error("צריך לבחור תאריך למשימה.");
    }
    return {
      name: trimmedName,
      description: trimmedDescription,
      assigneeId: numericAssigneeId,
      type,
      taskDate,
      days: null
    };
  }

  const isEdit = Boolean(id);
  const days = isEdit
    ? (DAY_VALUES.includes(singleDay) ? [singleDay] : [])
    : [...new Set((selectedDays || []).filter(day => DAY_VALUES.includes(day)))];

  if (!days.length) {
    throw new Error(
      isEdit ? "צריך לבחור יום בשבוע." : "צריך לבחור לפחות יום אחד בשבוע."
    );
  }
  if (isEdit && days.length !== 1) {
    throw new Error("בעדכון משימה שבועית אפשר לבחור יום אחד בלבד.");
  }

  return {
    name: trimmedName,
    description: trimmedDescription,
    assigneeId: numericAssigneeId,
    type,
    taskDate: null,
    days
  };
}

export function renderWeeklyTasks(data = state.weeklyStats) {
  const chart = $("#weeklyTaskChart");
  chart.replaceChildren();
  if (!data) return;

  const members = [...(data.members || [])].sort(
    (a, b) =>
      b.completedCount - a.completedCount ||
      a.username.localeCompare(b.username, "he")
  );
  const maximum = Math.max(0, ...members.map(m => m.completedCount));
  const total = members.reduce((sum, m) => sum + m.completedCount, 0);

  $("#weeklyTaskDates").textContent = `${formatShortUtcDate(
    data.weekStart
  )} – ${formatShortUtcDate(data.weekEnd)}`;
  $("#weeklyTaskTotal").textContent = `${total} משימות הושלמו ביחד`;
  $("#weeklyTaskMessage").textContent = total
    ? "כל הכבוד על העשייה המשותפת!"
    : "השבוע רק מתחיל — המשימה הבאה יכולה להיות שלך.";

  for (const member of members) {
    const leader = maximum > 0 && member.completedCount === maximum;
    const column = textElement(
      "li",
      `weekly-column${leader ? " weekly-leader" : ""}`,
      ""
    );
    column.setAttribute(
      "aria-label",
      `${member.username}: ${member.completedCount} משימות הושלמו השבוע${
        leader ? ", מקום ראשון" : ""
      }`
    );

    const value = textElement(
      "span",
      "weekly-column-value",
      String(member.completedCount)
    );
    if (leader) {
      const trophy = textElement("span", "weekly-trophy", "🏆");
      trophy.setAttribute("aria-hidden", "true");
      value.prepend(trophy);
    }

    const track = textElement("div", "weekly-bar-track", "");
    track.setAttribute("aria-hidden", "true");

    const bar = textElement(
      "div",
      `weekly-bar${member.completedCount ? "" : " weekly-zero"}`,
      ""
    );
    bar.style.height = `${maximum ? (member.completedCount / maximum) * 100 : 0}%`;
    track.append(bar);

    column.append(
      value,
      track,
      avatar(member.username),
      textElement("span", "weekly-column-name", member.username)
    );
    chart.append(column);
  }
}

export function buildWeeklySchedule(
  tasks,
  assignees,
  { today = israelToday(), currentUserId = state.user?.id } = {}
) {
  const weekDays = getWeekDays(today);
  const weekStart = weekDays[0].isoDate;
  const weekEnd = weekDays[6].isoDate;

  const sortedAssignees = [...(assignees || [])].sort((a, b) =>
    String(a.username || "").localeCompare(String(b.username || ""), "he")
  );

  const rows = sortedAssignees.map(member => ({
    id: member.id,
    username: member.username,
    isCurrentUser:
      currentUserId !== undefined &&
      currentUserId !== null &&
      String(member.id) === String(currentUserId),
    isUnassigned: false,
    cells: Array.from({ length: 7 }, () => []),
    totalCount: 0
  }));

  const rowById = new Map(rows.map(row => [String(row.id), row]));
  const rowByUsername = new Map(rows.map(row => [row.username, row]));
  const unassignedRow = {
    id: "unassigned",
    username: "ללא אחראי",
    isCurrentUser: false,
    isUnassigned: true,
    cells: Array.from({ length: 7 }, () => []),
    totalCount: 0
  };

  let totalScheduled = 0;

  for (const task of tasks || []) {
    const type = taskType(task);
    let targetDayIndex = -1;

    if (type === "WEEKLY") {
      targetDayIndex = taskDayIndex(task);
    } else if (type === "ONETIME") {
      if (
        task.taskDate &&
        task.taskDate >= weekStart &&
        task.taskDate <= weekEnd
      ) {
        targetDayIndex = weekDays.findIndex(d => d.isoDate === task.taskDate);
      }
    }

    if (targetDayIndex < 0 || targetDayIndex > 6) continue;

    const assignee = taskAssignee(task, assignees);
    const targetRow =
      (assignee.id !== null && rowById.get(String(assignee.id))) ||
      (assignee.username && rowByUsername.get(assignee.username)) ||
      unassignedRow;

    targetRow.cells[targetDayIndex].push(task);
    targetRow.totalCount += 1;
    totalScheduled += 1;
  }

  const allRows = unassignedRow.totalCount > 0 ? [...rows, unassignedRow] : rows;

  for (const row of allRows) {
    for (const cell of row.cells) {
      cell.sort(
        (a, b) =>
          Number(a.completed) - Number(b.completed) || (a.id || 0) - (b.id || 0)
      );
    }
  }

  return { weekDays, rows: allRows, totalScheduled };
}

export function renderWeeklySchedule({ onReload } = {}) {
  const today = israelToday();
  const { weekDays, rows } = buildWeeklySchedule(
    state.tasks,
    state.assignees,
    { today, currentUserId: state.user?.id }
  );

  $("#weeklyScheduleDates").textContent = `${formatShortUtcDate(
    weekDays[0].isoDate
  )} – ${formatShortUtcDate(weekDays[6].isoDate)}`;

  const head = $("#weeklyScheduleHead");
  const body = $("#weeklyScheduleBody");
  head.replaceChildren();
  body.replaceChildren();

  const hasMembers = rows.length > 0;
  $("#weeklyScheduleScroll").hidden = !hasMembers;
  $("#weeklyScheduleEmpty").hidden = hasMembers;
  if (!hasMembers) return;

  const headRow = document.createElement("tr");
  const memberHeader = textElement("th", "schedule-member-col", "בן / בת בית");
  memberHeader.setAttribute("scope", "col");
  headRow.append(memberHeader);

  weekDays.forEach(day => {
    const th = document.createElement("th");
    th.setAttribute("scope", "col");
    th.className = `schedule-day-col${day.isToday ? " is-today" : ""}`;
    th.append(
      textElement("span", "schedule-day-name", `יום ${day.dayName}`),
      textElement("span", "schedule-day-date", day.displayDate)
    );
    headRow.append(th);
  });
  head.append(headRow);

  rows.forEach(row => {
    const tr = document.createElement("tr");
    tr.className = row.isCurrentUser ? "is-current-user" : "";

    const memberCell = document.createElement("th");
    memberCell.setAttribute("scope", "row");
    memberCell.className = "schedule-member-cell";

    const memberWrap = textElement("div", "schedule-member-info", "");
    if (!row.isUnassigned) {
      memberWrap.append(avatar(row.username));
    }
    const nameGroup = textElement("div", "schedule-member-text", "");
    const nameLine = textElement("div", "schedule-member-name", row.username);
    if (row.isCurrentUser) {
      nameLine.append(textElement("span", "schedule-me-pill", "אני"));
    }
    nameGroup.append(
      nameLine,
      textElement(
        "span",
        "schedule-member-count",
        row.totalCount ? `${row.totalCount} משימות השבוע` : "פנוי השבוע"
      )
    );
    memberWrap.append(nameGroup);
    memberCell.append(memberWrap);
    tr.append(memberCell);

    row.cells.forEach((cellTasks, dayIdx) => {
      const day = weekDays[dayIdx];
      const td = document.createElement("td");
      td.className = `schedule-day-cell${day.isToday ? " is-today" : ""}`;
      td.dataset.label = `יום ${day.dayName} · ${day.displayDate}`;

      if (!cellTasks.length) {
        const empty = textElement("span", "schedule-empty-cell", "—");
        empty.setAttribute("aria-label", "אין משימות");
        td.append(empty);
      } else {
        const list = textElement("ul", "schedule-task-list", "");
        cellTasks.forEach(task => {
          const weekly = taskType(task) === "WEEKLY";
          const isOverdue =
            !task.completed && !weekly && task.taskDate && task.taskDate < today;
          const li = document.createElement("li");
          const card = textElement(
            "div",
            `schedule-task ${weekly ? "recurring" : "onetime"}${
              task.completed ? " completed" : ""
            }${isOverdue ? " overdue" : ""}`,
            ""
          );

          const top = textElement("div", "schedule-task-top", "");
          top.append(textElement("span", "schedule-task-name", task.name));

          const actions = textElement("div", "schedule-task-actions", "");
          if (!task.completed && isMyTask(task)) {
            const completeBtn = document.createElement("button");
            completeBtn.type = "button";
            completeBtn.className = "schedule-complete-btn";
            completeBtn.append(createIcon("check"));
            completeBtn.setAttribute(
              "aria-label",
              `סימון המשימה ${task.name} כהושלמה`
            );
            completeBtn.title = "סימון כהושלמה";
            completeBtn.addEventListener("click", () =>
              runButtonAction(completeBtn, async () => {
                await api(`/tasks/${task.id}/complete`, "PATCH");
                notify("עוד משימה מאחורינו. תודה על החלק שלך!");
                if (onReload) await onReload();
              })
            );
            actions.append(completeBtn);
          }
          if (canManageTasks()) {
            const editBtn = actionButton(
              "edit",
              `עריכת המשימה ${task.name}`,
              () => openTask(task)
            );
            editBtn.classList.add("schedule-edit-btn");
            actions.append(editBtn);
          }
          if (actions.children.length) top.append(actions);

          const meta = textElement("div", "schedule-task-meta", "");
          meta.append(
            textElement(
              "span",
              `schedule-task-kind ${weekly ? "recurring" : "onetime"}`,
              weekly ? "חוזרת" : "חד־פעמית"
            )
          );
          if (task.completed) {
            meta.append(textElement("span", "schedule-task-status done", "בוצע"));
          } else if (isOverdue) {
            meta.append(
              textElement("span", "schedule-task-status overdue", "באיחור")
            );
          }

          card.append(top, meta);
          li.append(card);
          list.append(li);
        });
        td.append(list);
      }

      tr.append(td);
    });

    body.append(tr);
  });
}

export function renderTasks({ onReload, onShowEmpty }) {
  const query = $("#searchInput").value;
  const day = $("#dayFilter").value;
  const type = $("#taskTypeFilter").value;
  const today = israelToday();

  const tasks = filterAndSortTasks(state.tasks, {
    query,
    day,
    type,
    filter: state.filter,
    today,
    user: state.user,
    assignees: state.assignees
  });

  setTableHeaders([
    "המשימה",
    "מועד",
    "באחריות",
    "מצב",
    { srOnly: "פעולות" }
  ]);

  const body = $("#tableBody");
  body.replaceChildren();

  tasks.forEach(task => {
    const weekly = taskType(task) === "WEEKLY";
    const assignee = taskAssignee(task);
    const row = createTableRow([
      { className: "item-main" },
      { label: "מועד" },
      { label: "באחריות" },
      { label: "מצב" },
      { actions: true }
    ]);
    row.classList.toggle("completed-row", Boolean(task.completed));

    row.children[0].append(
      textElement("span", "item-title", task.name),
      textElement(
        "span",
        `task-kind${weekly ? " recurring" : ""}`,
        weekly ? "חוזרת בכל שבוע" : "חד־פעמית"
      )
    );
    if (task.description) {
      row.children[0].append(
        textElement("span", "item-description", task.description)
      );
    }

    if (weekly) {
      row.children[1].append(
        textElement("span", "day-tag", `כל ${dayLabel(task.day)}`)
      );
    } else if (task.taskDate) {
      const time = textElement(
        "time",
        "task-due-date",
        formatIsoAsDisplayDate(task.taskDate)
      );
      time.dateTime = task.taskDate;
      row.children[1].append(
        time,
        textElement(
          "span",
          "item-description",
          formatHebrewDateFromIso(task.taskDate)
        )
      );
      if (!task.completed) {
        if (task.taskDate < today) {
          row.children[1].append(textElement("span", "task-overdue", "באיחור"));
        } else if (task.taskDate === today) {
          row.children[1].append(textElement("span", "task-today", "היום"));
        }
      }
    } else {
      row.children[1].append(textElement("span", "muted", "לא נקבע תאריך"));
    }

    const person = document.createElement("span");
    person.className = "person";
    if (assignee.username) person.append(avatar(assignee.username));
    person.append(
      textElement("span", "", assignee.username || "פרטי אחראי לא זמינים")
    );
    row.children[2].append(person);

    row.children[3].append(
      textElement(
        "span",
        `badge${task.completed ? " done" : ""}`,
        task.completed ? "הושלמה" : "לביצוע"
      )
    );

    const actions = row.querySelector(".row-actions");
    if (!task.completed && isMyTask(task)) {
      const complete = document.createElement("button");
      complete.type = "button";
      complete.className = "complete-button";
      complete.append(createIcon("check"), textElement("span", "", "סיימתי"));
      complete.setAttribute("aria-label", `סימון המשימה ${task.name} כהושלמה`);
      complete.addEventListener("click", () =>
        runButtonAction(complete, async () => {
          await api(`/tasks/${task.id}/complete`, "PATCH");
          notify("עוד משימה מאחורינו. תודה על החלק שלך!");
          await onReload();
        })
      );
      actions.append(complete);
    } else if (task.completed && !canManageTasks()) {
      actions.append(textElement("span", "completed-note", "בוצע"));
    }

    if (canManageTasks()) {
      actions.append(
        actionButton("edit", `עריכת המשימה ${task.name}`, () => openTask(task))
      );
      actions.append(
        actionButton(
          "trash",
          `מחיקת המשימה ${task.name}`,
          () =>
            confirmAction(
              weekly ? "למחוק את המשימה השבועית?" : "למחוק את המשימה?",
              weekly
                ? `המשימה ״${task.name}״ תוסר מהלוח ולא תחזור בשבועות הבאים.`
                : `המשימה ״${task.name}״ תוסר מהלוח של כל המשפחה.`,
              async () => {
                await api(`/tasks/${task.id}`, "DELETE");
                notify("המשימה הוסרה מהלוח.");
              }
            ),
          true
        )
      );
    }

    body.append(row);
  });

  $("#boardCount").textContent =
    `${tasks.length} משימות מוצגות · ${state.tasks.length} בסך הכול`;

  const hasActiveFilters = Boolean(
    query.trim() || day !== "all" || type !== "all" || state.filter !== "all"
  );
  onShowEmpty(tasks.length, hasActiveFilters);
}

export function openTask(task) {
  if (!canManageTasks()) return;
  const form = $("#taskForm");
  resetForm(form);
  form.elements.namedItem("id").value = task?.id || "";
  $("#taskDialogTitle").textContent = task ? "עריכת המשימה" : "משימה חדשה";
  $("#taskName").value = task?.name || "";
  $("#taskDescription").value = task?.description || "";
  $("#taskType").value = task ? taskType(task) : "ONETIME";
  $("#taskType").disabled = Boolean(task);
  $("#taskTypeHelp").textContent = task
    ? "סוג המשימה קבוע. כדי לבחור סוג אחר, צרו משימה חדשה."
    : "חד־פעמית לתאריך מסוים, או משימה שחוזרת ביום קבוע בכל שבוע.";

  const today = israelToday();
  $("#taskDate").value = task?.taskDate || today;
  const todayDay = new Date(`${today}T00:00:00Z`).getUTCDay();
  const selectedDay = task ? dayIndex(task.day) : todayDay;
  const defaultDayValue = DAY_VALUES[selectedDay >= 0 ? selectedDay : todayDay];
  $("#taskDay").value = defaultDayValue;
  $$('#taskDaysGroup input[name="days"]').forEach(checkbox => {
    checkbox.checked = !task && checkbox.value === defaultDayValue;
  });
  syncTaskScheduleFields(Boolean(task));

  const select = $("#taskAssignee");
  select.replaceChildren(new Option("בחירת אחראי", ""));
  state.assignees.forEach(member =>
    select.append(new Option(member.username, member.id))
  );

  const assignee = task ? taskAssignee(task) : null;
  const selected =
    assignee &&
    state.assignees.find(
      member =>
        (assignee.id !== null && String(member.id) === String(assignee.id)) ||
        (assignee.username && member.username === assignee.username)
    );
  select.value = selected ? String(selected.id) : "";
  $("#taskAssigneeHelp").hidden = !task || Boolean(selected);

  $("#taskDialog").showModal();
  $("#taskName").focus();
}

export function initTasks({ onReload }) {
  $("#taskType").addEventListener("change", () => syncTaskScheduleFields());

  handleForm($("#taskForm"), async form => {
    const id = form.elements.namedItem("id").value;
    const type = $("#taskType").value;
    const existing =
      id && state.tasks.find(task => String(task.id) === String(id));
    if (existing && taskType(existing) !== type) {
      throw new Error("אי אפשר לשנות את סוג המשימה.");
    }

    const selectedDays = $$('#taskDaysGroup input[name="days"]:checked').map(
      checkbox => checkbox.value
    );
    const body = buildTaskPayload({
      id,
      name: $("#taskName").value,
      description: $("#taskDescription").value,
      assigneeId: $("#taskAssignee").value,
      type,
      taskDate: $("#taskDate").value,
      singleDay: $("#taskDay").value,
      selectedDays
    });

    const result = await api(
      id ? `/tasks/${id}` : "/tasks",
      id ? "PUT" : "POST",
      body
    );
    $("#taskDialog").close();
    if (!id && Array.isArray(result) && result.length > 1) {
      notify(`נוספו ${result.length} משימות שבועיות ללוח המשפחתי.`);
    } else {
      notify(id ? "המשימה עודכנה." : "המשימה נוספה ללוח המשפחתי.");
    }
    await onReload();
  });
}
