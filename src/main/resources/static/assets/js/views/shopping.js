import { api } from "../api.js";
import { formatIsraelDateTime } from "../dates.js";
import {
  $,
  actionButton,
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
import { state } from "../state.js";

export const STALE_SHOPPING_MS = 7 * 24 * 60 * 60 * 1000;
const MAX_QUANTITY = 2147483647;

export function isStaleShoppingItem(item, nowMs = Date.now()) {
  const time = new Date(item?.updatedAt).getTime();
  if (Number.isNaN(time)) return false;
  return nowMs - time >= STALE_SHOPPING_MS;
}

export function filterShoppingItems(items, query = "") {
  const normalized = query.trim().toLowerCase();
  if (!normalized) return [...items];
  return items.filter(item =>
    String(item.productName || "").toLowerCase().includes(normalized)
  );
}

export function renderShopping({ onReload, onShowEmpty }) {
  const query = $("#searchInput").value;
  const items = filterShoppingItems(state.shopping, query);

  setTableHeaders([
    "המוצר",
    "כמות",
    "עדכון אחרון",
    { srOnly: "פעולות" }
  ]);

  const body = $("#tableBody");
  body.replaceChildren();

  items.forEach(item => {
    const row = createTableRow([
      { className: "item-main" },
      { label: "כמות" },
      { className: "date-cell", label: "עדכון אחרון" },
      { actions: true }
    ]);

    row.children[0].append(textElement("span", "item-title", item.productName));

    const stepper = textElement("div", "quantity-stepper", "");
    const decBtn = document.createElement("button");
    decBtn.type = "button";
    decBtn.className = "stepper-button";
    decBtn.append(createIcon("minus"));
    decBtn.setAttribute("aria-label", `הפחתת כמות ל${item.productName}`);
    decBtn.title = "הפחתת כמות";
    decBtn.disabled = item.quantity <= 1;

    const qtyBadge = textElement("span", "quantity-badge", String(item.quantity));

    const incBtn = document.createElement("button");
    incBtn.type = "button";
    incBtn.className = "stepper-button";
    incBtn.append(createIcon("plus"));
    incBtn.setAttribute("aria-label", `הוספת יחידה ל${item.productName}`);
    incBtn.title = "הוספת יחידה";
    incBtn.disabled = item.quantity >= MAX_QUANTITY;

    const updateQty = delta => {
      const nextQty = item.quantity + delta;
      if (nextQty < 1 || nextQty > MAX_QUANTITY) return;
      runButtonAction([decBtn, incBtn], async () => {
        await api(`/shopping-items/${item.id}`, "PUT", {
          productName: item.productName,
          quantity: nextQty
        });
        notify(`הכמות של ${item.productName} עודכנה ל־${nextQty}.`);
        await onReload();
      });
    };

    decBtn.addEventListener("click", () => updateQty(-1));
    incBtn.addEventListener("click", () => updateQty(1));

    stepper.append(decBtn, qtyBadge, incBtn);
    row.children[1].append(stepper);

    const time = textElement("time", "", formatIsraelDateTime(item.updatedAt));
    if (item.updatedAt) time.dateTime = item.updatedAt;
    row.children[2].append(time);

    if (isStaleShoppingItem(item)) {
      row.children[2].append(
        textElement("small", "", "לא עודכן שבוע · ממתין לניקוי")
      );
    }

    const actions = row.querySelector(".row-actions");
    actions.append(
      actionButton("edit", `עדכון המוצר ${item.productName}`, () =>
        openShopping(item)
      ),
      actionButton(
        "trash",
        `מחיקת המוצר ${item.productName}`,
        () =>
          confirmAction(
            "להסיר מרשימת הקניות?",
            `המוצר ״${item.productName}״ יוסר מהרשימה המשותפת.`,
            async () => {
              await api(`/shopping-items/${item.id}`, "DELETE");
              notify("המוצר הוסר מרשימת הקניות.");
            }
          ),
        true
      )
    );

    body.append(row);
  });

  $("#boardCount").textContent =
    `${items.length} מוצרים מוצגים · ${state.shopping.length} בסך הכול`;
  onShowEmpty(items.length, Boolean(query.trim()));
}

export function openShopping(item) {
  const form = $("#shoppingForm");
  resetForm(form);
  form.elements.namedItem("id").value = item?.id || "";
  $("#shoppingDialogTitle").textContent = item ? "עדכון מוצר" : "מה חסר בבית?";
  $("#productName").value = item?.productName || "";
  $("#productQuantity").value = item?.quantity || 1;
  $("#shoppingDialog").showModal();
  $("#productName").focus();
}

export function initShopping({ onReload }) {
  handleForm($("#shoppingForm"), async form => {
    const id = form.elements.namedItem("id").value;
    const body = {
      productName: $("#productName").value.trim(),
      quantity: Number($("#productQuantity").value)
    };
    if (!body.productName) throw new Error("צריך למלא שם מוצר.");
    if (!Number.isInteger(body.quantity) || body.quantity < 1) {
      throw new Error("הכמות חייבת להיות מספר שלם גדול מאפס.");
    }
    await api(
      id ? `/shopping-items/${id}` : "/shopping-items",
      id ? "PUT" : "POST",
      body
    );
    $("#shoppingDialog").close();
    notify(
      id
        ? "המוצר עודכן, יחד עם תאריך העדכון."
        : "המוצר נוסף לרשימת הקניות."
    );
    await onReload();
  });
}
