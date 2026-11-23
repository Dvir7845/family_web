import { currentIsraelMonth } from "./dates.js";

export const VALID_VIEWS = ["tasks", "shopping", "events", "vehicles"];

export function parseHashView(hash) {
  const candidate = String(hash || "").replace(/^#/, "");
  return VALID_VIEWS.includes(candidate) ? candidate : "tasks";
}

export const state = {
  view: parseHashView(typeof location !== "undefined" ? location.hash : ""),
  user: null,
  weeklyStats: null,
  vehicles: [],
  bookings: [],
  vehicleId: "",
  vehicleMonth: currentIsraelMonth(),
  tasks: [],
  shopping: [],
  events: [],
  assignees: [],
  filter: "all",
  loaded: false,
  loading: false,
  pendingDelete: null
};

export function isAdmin(user = state.user) {
  return user?.role === "ADMIN";
}

export function canManageTasks(user = state.user) {
  return isAdmin(user) || Boolean(user?.taskManager);
}

export function canUseVehicles(user = state.user) {
  return Boolean(user?.driver);
}

export function canManageVehicles(user = state.user) {
  return isAdmin(user) && canUseVehicles(user);
}
