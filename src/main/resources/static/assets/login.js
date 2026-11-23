"use strict";

const form = document.querySelector("#loginForm");
const message = document.querySelector("#loginMessage");
const submit = document.querySelector("#loginSubmit");
const retry = document.querySelector("#retryLogin");
const parameters = new URLSearchParams(window.location.search);

if (parameters.has("error")) {
  message.textContent = "שם המשתמש או הסיסמה אינם נכונים. אפשר לנסות שוב.";
  message.hidden = false;
} else if (parameters.has("passwordChanged")) {
  message.textContent = "הסיסמה שונתה. התחברו עם הסיסמה החדשה.";
  message.classList.add("success");
  message.hidden = false;
} else if (parameters.has("logout")) {
  message.textContent = "יצאת מהחשבון. אפשר לחזור בכל רגע.";
  message.classList.add("success");
  message.hidden = false;
} else if (parameters.has("expired")) {
  message.textContent = "ההתחברות הסתיימה. צריך להיכנס שוב כדי להמשיך.";
  message.hidden = false;
}

async function prepareLogin() {
  retry.hidden = true;
  submit.disabled = true;
  submit.querySelector("span").textContent = "כניסה לבית שלנו";
  try {
    const response = await fetch("/csrf", {
      credentials: "same-origin", cache: "no-store",
      headers: { Accept: "application/json" }
    });
    if (!response.ok) throw new Error("Connection unavailable");
    const csrf = await response.json();
    if (!csrf.token || !csrf.parameterName) throw new Error("Missing CSRF token");
    const input = document.querySelector("#csrfInput");
    input.name = csrf.parameterName;
    input.value = csrf.token;
    submit.disabled = false;
  } catch {
    message.textContent = "לא הצלחנו להתחבר לשרת. בדקו שהוא פועל ונסו שוב.";
    message.classList.remove("success");
    message.hidden = false;
    retry.hidden = false;
  }
}

retry.addEventListener("click", prepareLogin);
form.addEventListener("submit", () => {
  submit.disabled = true;
  submit.querySelector("span").textContent = "נכנסים…";
});
document.querySelector("#togglePassword").addEventListener("click", event => {
  const input = document.querySelector("#password");
  const visible = input.type === "password";
  input.type = visible ? "text" : "password";
  event.currentTarget.setAttribute("aria-pressed", String(visible));
  event.currentTarget.setAttribute("aria-label", visible ? "הסתרת הסיסמה" : "הצגת הסיסמה");
  const icon = document.querySelector("#togglePasswordIcon");
  if (icon) icon.setAttribute("href", visible ? "/assets/icons.svg#eye-off" : "/assets/icons.svg#eye");
});
window.addEventListener("pageshow", prepareLogin);
