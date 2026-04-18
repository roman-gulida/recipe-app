let RECIPE_META = { cuisines: [], difficulties: [] };
let ALL_RECIPES = [];

// NAVIGATION
document.querySelectorAll(".nav-link").forEach((link) => {
  link.addEventListener("click", (e) => {
    e.preventDefault();
    navigateTo(link.dataset.page);
  });
});

function navigateTo(pageId) {
  document
    .querySelectorAll(".nav-link")
    .forEach((l) => l.classList.remove("active"));
  document
    .querySelectorAll(".page")
    .forEach((p) => p.classList.remove("active"));

  const link = document.querySelector(`[data-page="${pageId}"]`);
  const page = document.getElementById(`page-${pageId}`);
  if (link) link.classList.add("active");
  if (page) page.classList.add("active");

  switch (pageId) {
    case "all-recipes":
      loadAllRecipes();
      break;
    case "xsl-view":
      initXslPage();
      break;
    case "recommend":
      loadRecommend("skill");
      break;
    case "by-cuisine":
      initCuisinePage();
      break;
    case "users":
      loadUsers();
      break;
  }
}

// BOOT
async function boot() {
  RECIPE_META = await api("/api/meta");
  buildFormDropdowns();

  navigateTo("all-recipes");
}

async function api(url, options = {}) {
  const res = await fetch(url, options);
  if (options.returnText) return res.text();
  return res.json();
}

// ALL RECIPES
async function loadAllRecipes() {
  const container = document.getElementById("recipes-container");
  container.innerHTML = '<p class="loading">Loading recipes…</p>';
  const recipes = await api("/api/recipes");
  ALL_RECIPES = recipes;
  renderRecipeCards(container, recipes);

  document.getElementById("recipe-search").oninput = function () {
    const q = this.value.toLowerCase();
    const filtered = recipes.filter(
      (r) =>
        r.title.toLowerCase().includes(q) ||
        r.cuisine1.toLowerCase().includes(q) ||
        r.cuisine2.toLowerCase().includes(q) ||
        r.difficulty.toLowerCase().includes(q),
    );
    renderRecipeCards(container, filtered);
  };
}

function renderRecipeCards(container, recipes) {
  if (!recipes.length) {
    container.innerHTML = '<p class="no-results">No recipes found.</p>';
    return;
  }
  container.innerHTML = recipes
    .map(
      (r) => `
    <div class="recipe-card ${r.difficulty.toLowerCase()}" onclick="showRecipeDetail('${r.id}')">
      <h3>${r.title}</h3>
      <div class="recipe-tags">
        <span class="tag">${r.cuisine1}</span>
        <span class="tag">${r.cuisine2}</span>
        <span class="tag difficulty">${r.difficulty}</span>
      </div>
    </div>
  `,
    )
    .join("");
}

// RECIPE DETAIL MODAL
async function showRecipeDetail(id) {
  const recipe = await api(`/api/recipes/${id}`);
  document.getElementById("modal-content").innerHTML = `
    <h2>${recipe.title}</h2>
    <div class="modal-field">
      <div class="mf-label">Recipe ID</div>
      <div class="mf-value">${recipe.id}</div>
    </div>
    <div class="modal-field">
      <div class="mf-label">Cuisine Types</div>
      <div class="mf-value">
        <span class="tag">${recipe.cuisine1}</span>
        &nbsp;
        <span class="tag">${recipe.cuisine2}</span>
      </div>
    </div>
    <div class="modal-field">
      <div class="mf-label">Difficulty Level</div>
      <div class="mf-value">
        <span class="difficulty-badge ${recipe.difficulty}">${recipe.difficulty}</span>
      </div>
    </div>
  `;
  document.getElementById("modal-overlay").classList.remove("hidden");
}

function closeModal() {
  document.getElementById("modal-overlay").classList.add("hidden");
}

// XSL VIEW
async function initXslPage() {
  // populate user dropdown
  const select = document.getElementById("xsl-user-select");
  const users = await api("/api/users");
  users.forEach((u) => {
    const opt = document.createElement("option");
    opt.value = u.id;
    opt.textContent = `${u.name} ${u.surname} (${u.skillLevel})`;
    select.appendChild(opt);
  });
  loadXslView();
}

async function loadXslView() {
  const userId = document.getElementById("xsl-user-select").value;
  const output = document.getElementById("xsl-output");
  output.innerHTML = '<p class="loading">Transforming XML with XSL…</p>';
  const html = await fetch(`/api/recipes/xsl-view?userId=${userId}`).then((r) =>
    r.text(),
  );
  output.innerHTML = html;
}

// RECOMMENDATIONS
async function loadRecommend(type, btn) {
  if (btn) {
    document
      .querySelectorAll(".tab-btn")
      .forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
  }

  const output = document.getElementById("recommend-output");
  output.innerHTML = '<p class="loading">Fetching recommendations…</p>';

  const data = await api(`/api/recommend/${type}`);

  const ucEl = document.getElementById("first-user-card");
  if (data.user) {
    const u = data.user;
    ucEl.innerHTML = `
      <div class="uc-name">${u.name} ${u.surname}</div>
      <div class="uc-meta">
        Skill Level: <strong>${u.skillLevel}</strong> | Preferred Cuisine: <strong>${u.preferredCuisine}</strong>
      </div>`;
    ucEl.classList.add("show");
  } else {
    ucEl.classList.remove("show");
  }

  renderRecipeCards(output, data.recipes || []);
}

// BY CUISINE
function initCuisinePage() {
  const container = document.getElementById("cuisine-buttons");
  container.innerHTML = RECIPE_META.cuisines
    .map(
      (c) => `
    <button class="cuisine-btn" onclick="filterByCuisine('${c}', this)">
      ${c}
    </button>
  `,
    )
    .join("");
}

async function filterByCuisine(cuisine, btn) {
  document
    .querySelectorAll(".cuisine-btn")
    .forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");

  const output = document.getElementById("cuisine-output");
  output.innerHTML = '<p class="loading">Loading…</p>';
  const recipes = await api(
    `/api/recipes/by-cuisine?cuisine=${encodeURIComponent(cuisine)}`,
  );
  renderRecipeCards(output, recipes);
}

// ADD RECIPE
async function submitRecipe() {
  const title = document.getElementById("r-title").value.trim();
  const cuisine1 = document.getElementById("r-cuisine1").value;
  const cuisine2 = document.getElementById("r-cuisine2").value;
  const difficulty = getSelectedRadio("r-diff");

  const errEl = document.getElementById("r-error");
  const succEl = document.getElementById("r-success");
  errEl.classList.add("hidden");
  succEl.classList.add("hidden");

  const res = await fetch("/api/recipes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, cuisine1, cuisine2, difficulty }),
  });
  const data = await res.json();

  if (!res.ok) {
    errEl.textContent = data.error || "Unknown error";
    errEl.classList.remove("hidden");
  } else {
    succEl.textContent = `Recipe saved with ID ${data.id}`;
    succEl.classList.remove("hidden");
    document.getElementById("r-title").value = "";
  }
}

// ADD USER
async function submitUser() {
  const name = document.getElementById("u-name").value.trim();
  const surname = document.getElementById("u-surname").value.trim();
  const skillLevel = getSelectedRadio("u-skill");
  const preferredCuisine = document.getElementById("u-cuisine").value;

  const errEl = document.getElementById("u-error");
  const succEl = document.getElementById("u-success");
  errEl.classList.add("hidden");
  succEl.classList.add("hidden");

  const res = await fetch("/api/users", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, surname, skillLevel, preferredCuisine }),
  });
  const data = await res.json();

  if (!res.ok) {
    errEl.textContent = data.error || "Unknown error";
    errEl.classList.remove("hidden");
  } else {
    succEl.textContent = `User saved with ID ${data.id}`;
    succEl.classList.remove("hidden");
    document.getElementById("u-name").value = "";
    document.getElementById("u-surname").value = "";
  }
}

// USERS LIST
async function loadUsers() {
  const container = document.getElementById("users-container");
  container.innerHTML = '<p class="loading">Loading users…</p>';
  const users = await api("/api/users");
  if (!users.length) {
    container.innerHTML = '<p class="no-results">No users yet</p>';
    return;
  }
  container.innerHTML = users
    .map(
      (u) => `
    <div class="user-card">
      <h3>${u.name} ${u.surname}</h3>
      <p>${u.id}</p>
      <p>Skill Level: <strong>${u.skillLevel}</strong></p>
      <p>Preferred Cuisine: <strong>${u.preferredCuisine}</strong></p>
    </div>
  `,
    )
    .join("");
}

// SCRAPER
async function runScraper() {
  const url = document.getElementById("scrape-url").value.trim();
  const resultEl = document.getElementById("scrape-result");
  resultEl.innerHTML = '<p class="loading">Scraping…</p>';

  const res = await fetch("/api/scrape", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ url }),
  });
  const data = await res.json();

  if (!res.ok) {
    resultEl.innerHTML = `<div class="alert alert-error">${data.error}</div>`;
  } else {
    resultEl.innerHTML = `
      <div class="alert alert-success">Added ${data.added} recipe(s).</div>
      ${data.recipes
        .map(
          (r) => `
        <div class="recipe-card ${r.difficulty.toLowerCase()}" style="margin-bottom:10px;">
          <h3>${r.title}</h3>
          <div class="recipe-tags">
            <span class="tag">${r.cuisine1}</span>
            <span class="tag">${r.cuisine2}</span>
            <span class="tag difficulty">${r.difficulty}</span>
          </div>
        </div>`,
        )
        .join("")}
    `;
  }
}

// FORM HELPERS
function buildFormDropdowns() {
  // cuisine selects for Add Recipe
  ["r-cuisine1", "r-cuisine2", "u-cuisine"].forEach((id) => {
    const sel = document.getElementById(id);
    if (!sel) return;
    RECIPE_META.cuisines.forEach((c) => {
      const opt = document.createElement("option");
      opt.value = c;
      opt.textContent = c;
      sel.appendChild(opt);
    });
    if (id === "r-cuisine2" && RECIPE_META.cuisines.length > 1)
      sel.selectedIndex = 1;
  });

  // difficulty radio groups
  buildRadioGroup("r-difficulty-group", "r-diff", RECIPE_META.difficulties);
  buildRadioGroup("u-skill-group", "u-skill", RECIPE_META.difficulties);
}

function buildRadioGroup(containerId, name, options) {
  const container = document.getElementById(containerId);
  if (!container) return;
  container.innerHTML = options
    .map(
      (opt, i) => `
    <label class="radio-label ${i === 0 ? "selected" : ""}" onclick="selectRadio(this, '${name}')">
      <input type="radio" name="${name}" value="${opt}" ${i === 0 ? "checked" : ""}/>
      ${opt}
    </label>
  `,
    )
    .join("");
}

function selectRadio(label, name) {
  document.querySelectorAll(`[name="${name}"]`).forEach((inp) => {
    inp.closest(".radio-label").classList.remove("selected");
  });
  label.classList.add("selected");
  label.querySelector("input").checked = true;
}

function getSelectedRadio(name) {
  const inp = document.querySelector(`input[name="${name}"]:checked`);
  return inp ? inp.value : "";
}

boot();
