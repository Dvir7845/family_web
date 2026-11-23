import { api } from "../api.js";
import {
  $,
  actionButton,
  avatar,
  confirmAction,
  handleForm,
  notify,
  resetForm,
  runButtonAction,
  textElement
} from "../dom.js";
import { isAdmin, state } from "../state.js";

export function renderMembers({ onReload }) {
  const list = $("#membersList");
  list.replaceChildren();

  state.assignees.forEach(member => {
    const card = textElement("article", "member-card", "");

    const header = textElement("div", "member-card-header", "");
    const identity = textElement("div", "member-identity", "");
    identity.append(
      avatar(member.username),
      textElement("strong", "member-name", member.username),
      textElement(
        "span",
        "member-role",
        member.role === "ADMIN" ? "מנהל" : "בן / בת משפחה"
      )
    );
    header.append(identity);

    if (member.id !== state.user.id) {
      header.append(
        actionButton(
          "trash",
          `מחיקת המשתמש ${member.username}`,
          () =>
            confirmAction(
              "למחוק את המשתמש?",
              `החשבון של ${member.username} יימחק. הזמנות הרכב שלו יבוטלו והמשימות יישארו ללא אחראי. לא ניתן לבטל את המחיקה.`,
              async () => {
                await api(`/members/${member.id}`, "DELETE");
                notify("המשתמש נמחק.");
              }
            ),
          true
        )
      );
    }

    const toggles = textElement("div", "member-toggles", "");

    const driverLabel = document.createElement("label");
    driverLabel.className = "driver-option";
    const driverCheckbox = document.createElement("input");
    driverCheckbox.type = "checkbox";
    driverCheckbox.checked = Boolean(member.driver);
    driverCheckbox.setAttribute("aria-label", `נהג: ${member.username}`);
    driverCheckbox.addEventListener("change", () =>
      runButtonAction(driverCheckbox, async () => {
        try {
          await api(`/members/${member.id}/driver`, "PATCH", {
            driver: driverCheckbox.checked
          });
        } finally {
          await onReload();
        }
      })
    );
    driverLabel.append(driverCheckbox, "נהג / נהגת");
    toggles.append(driverLabel);

    const tmLabel = document.createElement("label");
    tmLabel.className = "driver-option";
    const tmCheckbox = document.createElement("input");
    tmCheckbox.type = "checkbox";
    tmCheckbox.checked = Boolean(member.taskManager);
    tmCheckbox.setAttribute(
      "aria-label",
      `אחראי משימות: ${member.username}`
    );
    tmCheckbox.addEventListener("change", () =>
      runButtonAction(tmCheckbox, async () => {
        try {
          await api(`/members/${member.id}/task-manager`, "PATCH", {
            taskManager: tmCheckbox.checked
          });
        } finally {
          await onReload();
        }
      })
    );
    tmLabel.append(tmCheckbox, "אחראי / אחראית משימות");
    toggles.append(tmLabel);

    card.append(header, toggles);

    if (member.role !== "ADMIN" && member.id !== state.user.id) {
      const cardActions = textElement("div", "member-card-actions", "");
      const promote = textElement(
        "button",
        "button button-secondary promote-member",
        "הפיכה למנהל"
      );
      promote.type = "button";
      promote.addEventListener("click", () =>
        confirmAction(
          "להפוך את המשתמש למנהל?",
          `${member.username} יקבל הרשאות לניהול המשפחה, המשימות והמשתמשים. יהיה עליו להתחבר מחדש.`,
          async () => {
            await api(`/members/${member.id}/promote`, "POST");
            notify("המשתמש קודם למנהל.");
          },
          { confirmLabel: "כן, להפוך למנהל", danger: false }
        )
      );
      cardActions.append(promote);
      card.append(cardActions);
    }

    list.append(card);
  });
}

export function openMembersDialog({ onReload }) {
  if (!isAdmin()) return;
  resetForm($("#memberForm"));
  renderMembers({ onReload });
  $("#memberDialog").showModal();
  $("#memberUsername").focus();
}

export function openPasswordDialog() {
  resetForm($("#passwordForm"));
  $("#passwordDialog").showModal();
  $("#currentPassword").focus();
}

export function initMembers({ onReload }) {
  $("#openMembers").addEventListener("click", () =>
    openMembersDialog({ onReload })
  );

  $("#openPassword").addEventListener("click", openPasswordDialog);

  handleForm($("#passwordForm"), async () => {
    const newPassword = $("#newPassword").value;
    if (newPassword !== $("#confirmPassword").value) {
      throw new Error("הסיסמאות החדשות אינן תואמות.");
    }
    if (new TextEncoder().encode(newPassword).length > 72) {
      throw new Error("הסיסמה ארוכה מדי. נסו סיסמה קצרה יותר.");
    }
    await api("/me/password", "PATCH", {
      currentPassword: $("#currentPassword").value,
      newPassword
    });
    $("#passwordDialog").close();
    location.assign("/login.html?passwordChanged");
  });

  handleForm($("#memberForm"), async form => {
    const username = $("#memberUsername").value.trim();
    const password = $("#memberPassword").value;
    if (new TextEncoder().encode(password).length > 72) {
      throw new Error("הסיסמה ארוכה מדי. נסו סיסמה קצרה יותר.");
    }
    await api("/members", "POST", {
      username,
      password,
      driver: $("#memberDriver").checked,
      taskManager: $("#memberTaskManager").checked
    });
    form.reset();
    await onReload();
    notify(`החשבון של ${username} נוצר. אפשר להתחבר עם הפרטים שבחרתם.`);
  });
}
