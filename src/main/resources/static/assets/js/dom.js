import { state } from "./state.js";

const SVG_NS = "http://www.w3.org/2000/svg";
let toastTimer;

export const $ = (selector, root = document) => root.querySelector(selector);
export const $$ = (selector, root = document) =>
  Array.from(root.querySelectorAll(selector));

export function createIcon(name) {
  const createSvgElement =
    typeof document.createElementNS === "function"
      ? tag => document.createElementNS(SVG_NS, tag)
      : tag => document.createElement(tag);

  const svg = createSvgElement("svg");
  svg.setAttribute("class", "icon");
  svg.setAttribute("aria-hidden", "true");

  const use = createSvgElement("use");
  use.setAttribute("href", `/assets/icons.svg#${name}`);
  svg.append(use);
  return svg;
}

export function textElement(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  element.textContent = text;
  return element;
}

export function setTableHeaders(headers) {
  const thead = $("#tableHead");
  const row = document.createElement("tr");
  headers.forEach(header => {
    const th = document.createElement("th");
    th.setAttribute("scope", "col");
    if (typeof header === "string") {
      th.textContent = header;
    } else if (header?.srOnly) {
      th.append(textElement("span", "sr-only", header.srOnly));
    } else if (header?.label) {
      th.textContent = header.label;
    }
    row.append(th);
  });
  thead.replaceChildren(row);
}

export function createTableRow(cells) {
  const row = document.createElement("tr");
  cells.forEach(cell => {
    const td = document.createElement("td");
    if (cell.className) td.className = cell.className;
    if (cell.label) td.dataset.label = cell.label;
    if (cell.actions) {
      if (!cell.className) td.className = "item-actions";
      td.append(textElement("div", "row-actions", ""));
    }
    row.append(td);
  });
  return row;
}

export function setBoardNote(iconName, text) {
  $("#boardNote").replaceChildren(
    createIcon(iconName),
    textElement("span", "", text)
  );
}

export function avatar(username) {
  const element = document.createElement("span");
  element.className = "avatar";
  element.setAttribute("aria-hidden", "true");
  element.textContent = String(username || "?").slice(0, 1).toUpperCase();
  const hue =
    [...String(username || "")].reduce(
      (sum, char) => sum + char.charCodeAt(0),
      0
    ) % 100;
  element.style.backgroundColor = `hsl(${hue + 40} 23% 90%)`;
  element.style.color = `hsl(${hue + 40} 22% 40%)`;
  return element;
}

export function actionButton(iconName, label, onClick, danger = false) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `icon-button${danger ? " danger" : ""}`;
  button.append(createIcon(iconName));
  button.setAttribute("aria-label", label);
  button.title = label;
  button.addEventListener("click", onClick);
  return button;
}

export function notify(message, failed = false) {
  const toast = $("#toast");
  clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.toggle("failed", failed);
  toast.hidden = false;
  toastTimer = setTimeout(() => {
    toast.hidden = true;
  }, 4500);
}

export function showPageError(error) {
  const banner = $("#pageError");
  banner.textContent = error?.message || "לא הצלחנו לטעון את הלוח.";
  banner.hidden = false;
}

export function clearPageError() {
  const banner = $("#pageError");
  banner.hidden = true;
  banner.textContent = "";
}

export function resetForm(form) {
  if (!form) return;
  form.reset();
  const error = form.querySelector(".form-error");
  if (error) error.hidden = true;
}

export async function runButtonAction(buttons, action) {
  const list = Array.isArray(buttons) ? buttons : [buttons];
  list.forEach(button => {
    if (button) button.disabled = true;
  });
  try {
    await action();
  } catch (error) {
    notify(error.message, true);
  } finally {
    list.forEach(button => {
      if (button) button.disabled = false;
    });
  }
}

export function handleForm(form, action) {
  form.addEventListener("submit", async event => {
    event.preventDefault();
    if (form.dataset.busy === "true" || !form.reportValidity()) return;
    const errorArea = form.querySelector(".form-error");
    if (errorArea) errorArea.hidden = true;
    form.dataset.busy = "true";
    form.querySelectorAll("button").forEach(button => {
      button.disabled = true;
    });
    try {
      await action(form);
    } catch (error) {
      if (errorArea) {
        errorArea.textContent = error.message || "לא הצלחנו לשמור. נסו שוב.";
        errorArea.hidden = false;
      }
    } finally {
      form.dataset.busy = "false";
      form.querySelectorAll("button").forEach(button => {
        button.disabled = false;
      });
    }
  });
}

export function confirmAction(
  title,
  description,
  action,
  { confirmLabel = "כן, למחוק", danger = true } = {}
) {
  resetForm($("#confirmForm"));
  $("#confirmTitle").textContent = title;
  $("#confirmDescription").textContent = description;
  const submitButton = $("#confirmForm button[type=submit]");
  submitButton.textContent = confirmLabel;
  submitButton.classList.toggle("button-danger", danger);
  submitButton.classList.toggle("button-primary", !danger);
  state.pendingDelete = action;
  $("#confirmDialog").showModal();
}

export function wireDialogs() {
  $$("dialog").forEach(dialog => {
    dialog.querySelectorAll("[data-close]").forEach(button => {
      button.addEventListener("click", () => dialog.close());
    });
    dialog.addEventListener("cancel", event => {
      if (dialog.querySelector("form")?.dataset.busy === "true") {
        event.preventDefault();
      }
    });
    dialog.addEventListener("click", event => {
      if (
        event.target !== dialog ||
        dialog.querySelector("form")?.dataset.busy === "true"
      ) {
        return;
      }
      if (event.clientX === 0 && event.clientY === 0) return;
      const rect = dialog.getBoundingClientRect();
      const inside =
        event.clientX >= rect.left &&
        event.clientX <= rect.right &&
        event.clientY >= rect.top &&
        event.clientY <= rect.bottom;
      if (!inside) {
        dialog.close();
      }
    });
    dialog.addEventListener("close", () => {
      resetForm(dialog.querySelector("form"));
      if (dialog.id === "confirmDialog") {
        state.pendingDelete = null;
      }
    });
  });
}
