export const ISRAEL_TZ = "Asia/Jerusalem";
export const DAY_NAMES = ["ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"];
export const DAY_VALUES = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY"
];

const israelPartsFormatter = new Intl.DateTimeFormat("en-US", {
  timeZone: ISRAEL_TZ,
  year: "numeric",
  month: "2-digit",
  day: "2-digit"
});

const dateTimeFormatter = new Intl.DateTimeFormat("he-IL", {
  timeZone: ISRAEL_TZ,
  dateStyle: "short",
  timeStyle: "short"
});

const syncTimeFormatter = new Intl.DateTimeFormat("he-IL", {
  timeZone: ISRAEL_TZ,
  hour: "2-digit",
  minute: "2-digit"
});

const gregorianTodayFormatter = new Intl.DateTimeFormat("he-IL", {
  timeZone: ISRAEL_TZ,
  weekday: "long",
  day: "numeric",
  month: "long"
});

const shortUtcDateFormatter = new Intl.DateTimeFormat("he-IL", {
  day: "numeric",
  month: "long",
  timeZone: "UTC"
});

const hebrewDateFormatter = new Intl.DateTimeFormat("he-IL-u-ca-hebrew", {
  day: "numeric",
  month: "long",
  year: "numeric",
  timeZone: "UTC"
});

const HEBREW_LETTERS = [
  [400, "ת"],
  [300, "ש"],
  [200, "ר"],
  [100, "ק"],
  [90, "צ"],
  [80, "פ"],
  [70, "ע"],
  [60, "ס"],
  [50, "נ"],
  [40, "מ"],
  [30, "ל"],
  [20, "כ"],
  [10, "י"],
  [9, "ט"],
  [8, "ח"],
  [7, "ז"],
  [6, "ו"],
  [5, "ה"],
  [4, "ד"],
  [3, "ג"],
  [2, "ב"],
  [1, "א"]
];

export function israelDateParts(now = new Date()) {
  const parts = Object.fromEntries(
    israelPartsFormatter.formatToParts(now).map(part => [part.type, part.value])
  );
  return {
    year: parts.year,
    month: parts.month,
    day: parts.day
  };
}

export function israelToday(now = new Date()) {
  const { year, month, day } = israelDateParts(now);
  return `${year}-${month}-${day}`;
}

export function currentIsraelMonth(now = new Date()) {
  const { year, month } = israelDateParts(now);
  return `${year}-${month}`;
}

export function israelDateKey(date) {
  const parsed = date instanceof Date ? date : new Date(date);
  if (Number.isNaN(parsed.getTime())) return "";
  return israelToday(parsed);
}

export function formatIsraelDateTime(isoString) {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "—";
  return dateTimeFormatter.format(date);
}

export function formatSyncTime(now = new Date()) {
  return `רוענן ב־${syncTimeFormatter.format(now)}`;
}

export function formatIsoAsDisplayDate(isoDate) {
  if (!isoDate || typeof isoDate !== "string") return "";
  const parts = isoDate.split("-");
  if (parts.length !== 3) return isoDate;
  const [year, month, day] = parts;
  return `${day}/${month}/${year}`;
}

export function formatShortUtcDate(isoDate) {
  if (!isoDate) return "";
  return shortUtcDateFormatter.format(new Date(`${isoDate}T12:00:00Z`));
}

export function dayIndex(day) {
  const text = String(day || "").trim().replace(/^יום\s+/, "");
  const english = DAY_VALUES.findIndex(
    value => value.toLowerCase() === text.toLowerCase()
  );
  return english >= 0 ? english : DAY_NAMES.indexOf(text);
}

export function dayLabel(day) {
  const index = dayIndex(day);
  return index >= 0 ? `יום ${DAY_NAMES[index]}` : day || "לא נקבע יום";
}

export function toHebrewNumber(number) {
  let remaining = Number(number);
  if (!Number.isFinite(remaining) || remaining <= 0) return "";
  let result = "";

  for (const [value, letter] of HEBREW_LETTERS) {
    if (remaining === 15 || remaining === 16) {
      result += remaining === 15 ? "טו" : "טז";
      break;
    }

    while (remaining >= value) {
      result += letter;
      remaining -= value;
    }
  }

  if (!result) return "";
  if (result.length === 1) {
    return `${result}׳`;
  }
  return `${result.slice(0, -1)}״${result.slice(-1)}`;
}

export function formatHebrewDate(utcDate) {
  if (!(utcDate instanceof Date) || Number.isNaN(utcDate.getTime())) return "";
  const parts = Object.fromEntries(
    hebrewDateFormatter.formatToParts(utcDate).map(part => [part.type, part.value])
  );

  const year = Number(parts.year);
  const thousands = Math.floor(year / 1000);
  const remainder = year % 1000;

  const hebrewDay = toHebrewNumber(Number(parts.day));
  const hebrewYear =
    toHebrewNumber(thousands) + (remainder ? toHebrewNumber(remainder) : "");

  return `${hebrewDay} ב${parts.month} ${hebrewYear}`;
}

export function formatHebrewDateFromIso(isoDate) {
  if (!isoDate || typeof isoDate !== "string") return "";
  const [year, month, day] = isoDate.split("-").map(Number);
  if (!year || !month || !day) return "";
  return formatHebrewDate(new Date(Date.UTC(year, month - 1, day)));
}

export function formatHeaderToday(now = new Date()) {
  const { year, month, day } = israelDateParts(now);
  const todayDate = new Date(
    Date.UTC(Number(year), Number(month) - 1, Number(day))
  );
  return `${gregorianTodayFormatter.format(now)} · ${formatHebrewDate(todayDate)}`;
}

export function shiftYearMonth(yearMonth, deltaMonths) {
  if (typeof yearMonth !== "string" || !/^\d{4}-\d{2}$/.test(yearMonth)) {
    return yearMonth;
  }
  const [year, month] = yearMonth.split("-").map(Number);
  if (year < 1 || year > 9999 || month < 1 || month > 12) {
    return yearMonth;
  }
  const totalMonths = year * 12 + (month - 1) + deltaMonths;
  const minTotal = 1 * 12;
  const maxTotal = 9999 * 12 + 11;
  const clamped = Math.min(maxTotal, Math.max(minTotal, totalMonths));
  const nextYear = Math.floor(clamped / 12);
  const nextMonth = (clamped % 12) + 1;
  return `${String(nextYear).padStart(4, "0")}-${String(nextMonth).padStart(2, "0")}`;
}

export function getWeekDays(today = israelToday()) {
  const anchor = new Date(`${today}T12:00:00Z`);
  const offset = anchor.getUTCDay();
  const start = new Date(anchor);
  start.setUTCDate(anchor.getUTCDate() - offset);

  return DAY_VALUES.map((dayValue, index) => {
    const current = new Date(start);
    current.setUTCDate(start.getUTCDate() + index);
    const isoDate = current.toISOString().slice(0, 10);
    const [, month, day] = isoDate.split("-");
    return {
      dayIndex: index,
      dayValue,
      dayName: DAY_NAMES[index],
      isoDate,
      displayDate: `${day}/${month}`,
      isToday: isoDate === today
    };
  });
}
