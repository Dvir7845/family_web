export const ERROR_TRANSLATIONS = {
  "Current password is incorrect": "הסיסמה הנוכחית אינה נכונה.",
  "Cannot delete your own account": "לא ניתן למחוק את החשבון שבו מחוברים.",
  "Task date is required for one-time tasks": "צריך לבחור תאריך למשימה החד־פעמית.",
  "Day of the week is required for weekly tasks": "צריך לבחור יום למשימה השבועית.",
  "At least one day of the week is required for weekly tasks": "צריך לבחור לפחות יום אחד בשבוע למשימה השבועית.",
  "Exactly one day of the week is required when updating a weekly task": "בעדכון משימה שבועית אפשר לבחור יום אחד בלבד.",
  "Task date should not be provided for weekly tasks": "למשימה שבועית יש לבחור ימים בשבוע בלבד.",
  "Day of the week should not be provided for one-time tasks": "למשימה חד־פעמית יש לבחור תאריך בלבד.",
  "Days of the week should not be provided for one-time tasks": "למשימה חד־פעמית יש לבחור תאריך בלבד.",
  "Cannot change task type": "אי אפשר לשנות את סוג המשימה. אפשר ליצור משימה חדשה מהסוג הרצוי.",
  "Task name is required": "צריך למלא שם למשימה.",
  "Day is required": "צריך לבחור יום בשבוע.",
  "Assignee is required": "צריך לבחור אחראי למשימה.",
  "Product name is required": "צריך למלא שם מוצר.",
  "Product name must be at most 100 characters": "שם המוצר יכול להכיל עד 100 תווים.",
  "Quantity must be greater than zero": "הכמות חייבת להיות מספר שלם גדול מאפס.",
  "Username already exists": "שם המשתמש הזה כבר קיים. בחרו שם אחר.",
  "Selected assignee does not exist": "האחראי שנבחר אינו קיים. רעננו את רשימת בני המשפחה.",
  "Password exceeds the supported byte length": "הסיסמה ארוכה מדי. נסו סיסמה קצרה יותר.",
  "Description is required": "צריך למלא תיאור לאירוע.",
  "Description must be at most 500 characters": "תיאור האירוע יכול להכיל עד 500 תווים.",
  "Event date is required": "צריך לבחור תאריך לאירוע.",
  "Invalid month": "החודש שנבחר אינו תקין.",
  "Username must contain 3–30 letters, digits or underscores": "שם המשתמש חייב להכיל 3–30 אותיות באנגלית, ספרות או קו תחתון."
};

const FIELD_NAMES = new Set([
  "name",
  "day",
  "days",
  "assigneeId",
  "productName",
  "quantity",
  "username",
  "password",
  "currentPassword",
  "newPassword",
  "description",
  "type",
  "taskDate",
  "eventDate",
  "invitedUserIds",
  "vehicleId",
  "date",
  "slot"
]);

const STATUS_MESSAGES = {
  400: "חלק מהפרטים אינם תקינים. בדקו את השדות ונסו שוב.",
  401: "ההתחברות הסתיימה. צריך להיכנס שוב.",
  403: "אין הרשאה לפעולה הזאת, או שההתחברות השתנתה. נסו לרענן את העמוד.",
  404: "הפריט כבר לא נמצא או שהפעולה אינה זמינה עבורך. כדאי לרענן את הרשימה.",
  409: "הפרטים כבר תפוסים. ייתכן שהמשבצת הוזמנה בינתיים; רעננו ונסו שוב."
};

export function errorMessage(status, data) {
  const rawMessage = data?.message || data?.detail;
  if (rawMessage && ERROR_TRANSLATIONS[rawMessage]) {
    return ERROR_TRANSLATIONS[rawMessage];
  }

  if (status === 400 && data && typeof data === "object") {
    const source =
      data.errors && typeof data.errors === "object" && !Array.isArray(data.errors)
        ? data.errors
        : data;
    const messages = Object.entries(source)
      .filter(([key, value]) => FIELD_NAMES.has(key) && typeof value === "string")
      .map(([, value]) => ERROR_TRANSLATIONS[value] || value);
    if (messages.length) return messages.join(" ");
  }

  return STATUS_MESSAGES[status] || "לא הצלחנו להשלים את הפעולה. נסו שוב בעוד רגע.";
}

export const LIST_PAGE_SIZE = 500;

export function unwrapPage(payload) {
  if (Array.isArray(payload)) {
    return {
      items: payload,
      totalPages: 1,
      totalElements: payload.length,
      truncated: false
    };
  }
  if (Array.isArray(payload?.content)) {
    const totalPages =
      Number(payload.totalPages ?? payload.page?.totalPages ?? 1) || 1;
    const totalElements = Number(
      payload.totalElements ?? payload.page?.totalElements ?? payload.content.length
    );
    return {
      items: payload.content,
      totalPages,
      totalElements,
      truncated: totalPages > 1
    };
  }
  return { items: [], totalPages: 1, totalElements: 0, truncated: false };
}

export function unwrapList(payload) {
  return unwrapPage(payload).items;
}

export async function getCsrf() {
  const response = await fetch("/csrf", {
    credentials: "same-origin",
    cache: "no-store",
    headers: { Accept: "application/json" }
  });
  if (!response.ok) {
    throw new Error("לא הצלחנו להכין את החיבור. נסו לרענן את העמוד.");
  }
  const token = await response.json();
  if (!token.token || !token.headerName) {
    throw new Error("לא הצלחנו להכין את החיבור. נסו לרענן את העמוד.");
  }
  return token;
}

export async function api(path, method = "GET", body) {
  const headers = { Accept: "application/json" };
  if (method !== "GET") {
    const csrf = await getCsrf();
    headers[csrf.headerName] = csrf.token;
  }
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  let response;
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: "same-origin",
      cache: "no-store",
      ...(body !== undefined ? { body: JSON.stringify(body) } : {})
    });
  } catch {
    throw new Error("אין כרגע חיבור לשרת. בדקו שהוא פועל ונסו לרענן.");
  }

  if (
    response.status === 401 ||
    (response.redirected && new URL(response.url).pathname === "/login.html")
  ) {
    if (typeof location !== "undefined") {
      location.assign("/login.html?expired");
    }
    throw new Error("ההתחברות הסתיימה.");
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      /* Some server failures have no JSON body. */
    }
  }

  if (!response.ok) {
    throw new Error(errorMessage(response.status, data));
  }
  return data;
}
