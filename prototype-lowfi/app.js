const guides = [
  { key: "general", label: "Fis Gral", full: "Fisica General", stock: 6, fiadas: 492, compradas: 0, fisico: 366, qr: 120, cls: "g-general", difficulty: "facil" },
  { key: "fis1", label: "Fis I", full: "Fisica I", stock: 5, fiadas: 420, compradas: 0, fisico: 286, qr: 129, cls: "g-fis1", difficulty: "media" },
  { key: "fis2", label: "Fis II", full: "Fisica II", stock: 44, fiadas: 200, compradas: 0, fisico: 135, qr: 21, cls: "g-fis2", difficulty: "alta" },
  { key: "fis3", label: "Fis III", full: "Fisica III", stock: 50, fiadas: 200, compradas: 0, fisico: 137, qr: 13, cls: "g-fis3", difficulty: "mas alta" },
];

const week = {
  general: [0, 0, 0, 0, 0, 0],
  fis1: [2, 0, 1, 2, 0, 0],
  fis2: [0, 0, 0, 0, 0, 0],
  fis3: [1, 0, 0, 0, 0, 0],
};

const recent = [
  ["13:20", "Fis I", "Efectivo"],
  ["11:27", "Fis I", "QR"],
  ["15:58", "Fis I", "Efectivo"],
  ["17:27", "Fis I", "Efectivo"],
  ["17:18", "Fis III", "Efectivo"],
  ["11:40", "Fis Gral", "QR"],
];

function tag(g) {
  return `<span class="tag ${g.cls}">${g.label}</span>`;
}

function fillDashboard() {
  document.querySelector("#quick-sales").innerHTML = guides
    .map(
      (g) => `<tr>
        <td>${tag(g)}</td>
        <td><strong>${g.stock}</strong></td>
        <td><button class="mini-button">Vender</button></td>
        <td><button class="mini-button">Vender QR</button></td>
      </tr>`,
    )
    .join("");

  document.querySelector("#week-sales").innerHTML = guides
    .map((g) => `<tr><td>${tag(g)}</td>${week[g.key].map((n) => `<td>${n || ""}</td>`).join("")}</tr>`)
    .join("");

  document.querySelector("#recent-sales").innerHTML = recent
    .map((r) => {
      const g = guides.find((item) => item.label === r[1]);
      return `<tr><td>${r[0]}</td><td>${g ? tag(g) : r[1]}</td><td>${r[2]}</td></tr>`;
    })
    .join("");
}

function fillOtherScreens() {
  document.querySelector("#sale-choices").innerHTML = guides
    .map((g) => `<div class="choice-card">${tag(g)}<small>Dificultad: ${g.difficulty}</small></div>`)
    .join("");

  document.querySelector("#inventory-rows").innerHTML = guides
    .map(
      (g) => `<tr>
        <td>${tag(g)}</td>
        <td>${g.fiadas}</td>
        <td>${g.compradas}</td>
        <td>${g.fisico}</td>
        <td>${g.qr}</td>
        <td><strong>${g.stock}</strong></td>
        <td>${g.stock <= 10 ? "Bajo" : "Disponible"}</td>
      </tr>`,
    )
    .join("");

  document.querySelector("#student-cards").innerHTML = guides
    .map(
      (g) => `<article class="student-card">
        <div><strong>${g.full}</strong><br /><small>Compra presencial - efectivo o QR</small></div>
        <div>${tag(g)} <strong>Bs 35</strong></div>
      </article>`,
    )
    .join("");
}

function bindNavigation() {
  const titles = {
    dashboard: "Dashboard",
    venta: "Registrar venta",
    inventario: "Inventario",
    movimientos: "Movimientos de dinero",
    cierre: "Cierre de caja",
    pedidos: "Pedidos",
    consulta: "Consulta estudiante",
  };

  document.querySelectorAll(".nav-link").forEach((button) => {
    button.addEventListener("click", () => {
      const target = button.dataset.screen;
      document.querySelectorAll(".nav-link").forEach((item) => item.classList.remove("is-active"));
      document.querySelectorAll(".screen").forEach((screen) => screen.classList.remove("is-visible"));
      button.classList.add("is-active");
      document.querySelector(`#${target}`).classList.add("is-visible");
      document.querySelector("#screen-title").textContent = titles[target];
    });
  });
}

fillDashboard();
fillOtherScreens();
bindNavigation();
