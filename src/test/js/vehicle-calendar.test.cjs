"use strict";

const assert = require("node:assert/strict");
const { loadEsm } = require("./esm-loader.cjs");

class Element {
  constructor(tagName = "div") {
    this.tagName = tagName;
    this.children = [];
    this.dataset = {};
    this.style = {};
    this.listeners = {};
    this.classList = {
      toggle() {}
    };
  }

  append(...items) {
    this.children.push(...items);
  }

  replaceChildren(...items) {
    this.children = items;
  }

  setAttribute() {}

  removeAttribute() {}

  addEventListener(name, fn) {
    this.listeners[name] = fn;
  }
}

async function run() {
  const elements = new Map();
  global.document = {
    title: "",
    querySelector(selector) {
      if (!elements.has(selector)) {
        elements.set(selector, new Element());
      }
      return elements.get(selector);
    },
    querySelectorAll() {
      return [];
    },
    createElement(tag) {
      return new Element(tag);
    }
  };
  global.Option = class Option {
    constructor(text, value) {
      this.text = text;
      this.value = value;
    }
  };

  const { state } = await loadEsm("src/main/resources/static/assets/js/state.js");
  const { renderVehicles } = await loadEsm(
    "src/main/resources/static/assets/js/views/vehicles.js"
  );

  state.view = "vehicles";
  state.user = { id: 1, driver: true, role: "MEMBER" };
  state.vehicleMonth = "2096-02";
  state.vehicleId = "1";
  state.vehicles = [{ id: 1, name: "Car" }];
  state.bookings = [
    {
      id: 1,
      vehicleId: 1,
      userId: 2,
      username: "Other",
      date: "2096-02-01",
      slot: "MORNING"
    },
    {
      id: 2,
      vehicleId: 1,
      userId: 1,
      username: "Me",
      date: "2096-02-01",
      slot: "AFTERNOON"
    }
  ];

  renderVehicles();

  let days = elements
    .get("#vehicleCalendar")
    .children.filter(el => el.children.length === 4);
  assert.equal(days.length, 29, "leap February has 29 days");
  assert.equal(days[0].children[1].disabled, true, "another driver cannot cancel");
  assert.equal(
    days[0].children[2].disabled,
    false,
    "member can cancel own booking"
  );
  assert.ok(
    days[0].children[2].className.includes("mine"),
    "own booking gets mine class"
  );
  assert.equal(days[0].children[3].disabled, false, "free slot is available");
  assert.equal(
    elements.get("#primaryAction").hidden,
    true,
    "member cannot add vehicles"
  );

  state.user.role = "ADMIN";
  renderVehicles();
  assert.equal(
    elements.get("#primaryAction").hidden,
    false,
    "admin driver can add vehicles"
  );
  days = elements
    .get("#vehicleCalendar")
    .children.filter(el => el.children.length === 4);
  assert.equal(
    days[0].children[1].disabled,
    false,
    "admin can cancel another user's booking"
  );

  state.vehicleMonth = "2000-02";
  state.bookings = [];
  renderVehicles();
  days = elements
    .get("#vehicleCalendar")
    .children.filter(el => el.children.length === 4);
  assert.equal(days.length, 29, "leap year 2000 February has 29 days");
  assert.equal(
    days[0].children[1].disabled,
    true,
    "past free slot cannot be booked"
  );
  assert.equal(
    days[0].children[2].disabled,
    true,
    "past free afternoon slot cannot be booked"
  );
  assert.equal(
    days[0].children[3].disabled,
    true,
    "past free evening slot cannot be booked"
  );

  console.log("Calendar UI checks passed");
}

run().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
