const app = document.getElementById('app');

function h(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key === 'class') node.className = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else node.setAttribute(key, value);
  }
  for (const child of children.flat()) {
    if (child === null || child === undefined || child === false) continue;
    node.appendChild(typeof child === 'object' ? child : document.createTextNode(String(child)));
  }
  return node;
}

function money(value) {
  return Number(value || 0).toLocaleString(undefined, { style: 'currency', currency: 'EUR' });
}

function showError(message) {
  app.replaceChildren(h('p', { class: 'error' }, message));
}

function stat(label, value) {
  return h('div', { class: 'stat' }, h('span', {}, label), h('strong', {}, money(value)));
}

async function renderFamilies() {
  const [families, types] = await Promise.all([api.listFamilies(), api.familyTypes()]);
  const typeSelect = h('select', {}, types.map(t => h('option', { value: t }, t)));
  const error = h('p', { class: 'error' });
  const registerForm = h('form', { class: 'card inline', onsubmit: async (e) => {
    e.preventDefault();
    error.textContent = '';
    try {
      await api.registerFamily(typeSelect.value);
      renderFamilies();
    } catch (err) {
      error.textContent = err.message;
    }
  }}, h('h2', {}, 'Register a family'), typeSelect, h('button', { type: 'submit' }, 'Add family'), error);

  const list = families.length
    ? h('ul', {}, families.map(f => h('li', {},
        h('a', { href: `#/families/${f.id}` }, `${f.type} family #${f.id}`),
        ` · ${f.memberCount} member(s)`)))
    : h('p', {}, 'No families yet.');

  app.replaceChildren(h('section', {}, h('h2', {}, 'Families'), list, registerForm));
}

function transactionForm(familyId, budget) {
  const categorySelect = h('select', {}, budget.categories.map(c => h('option', { value: c.id }, c.name)));
  const amount = h('input', { type: 'number', step: '0.01', min: '0.01', required: '', placeholder: 'Amount' });
  const description = h('input', { type: 'text', maxlength: '255', placeholder: 'Description' });
  const date = h('input', { type: 'date' });
  const error = h('p', { class: 'error' });
  return h('form', { class: 'inline', onsubmit: async (e) => {
    e.preventDefault();
    error.textContent = '';
    try {
      await api.recordTransaction(familyId, budget.id, categorySelect.value, {
        amount: amount.value,
        description: description.value || null,
        date: date.value || null
      });
      renderDashboard(familyId);
    } catch (err) {
      error.textContent = err.message;
    }
  }}, h('h4', {}, 'Add transaction'), categorySelect, amount, description, date,
     h('button', { type: 'submit' }, 'Add'), error);
}

function budgetCard(familyId, b) {
  return h('article', { class: 'card' },
    h('h3', {}, b.name, ' ', b.overspent ? h('span', { class: 'badge' }, 'Overspent') : null),
    h('p', {}, `${money(b.spent)} of ${money(b.limitAmount)} (${b.percentUsed}%)`),
    h('div', { class: b.overspent ? 'bar over' : 'bar' },
      h('div', { class: 'fill', style: `width:${b.barPercent}%` })),
    h('ul', {}, b.categories.map(c => h('li', {}, `${c.name}: ${money(c.spent)}`))),
    transactionForm(familyId, b));
}

function budgetForm(familyId) {
  const limit = h('input', { type: 'number', step: '0.01', min: '0', required: '', placeholder: 'Monthly limit' });
  const error = h('p', { class: 'error' });
  return h('form', { class: 'card inline', onsubmit: async (e) => {
    e.preventDefault();
    error.textContent = '';
    try {
      await api.createBudget(familyId, limit.value);
      renderDashboard(familyId);
    } catch (err) {
      error.textContent = err.message;
    }
  }}, h('h3', {}, 'New budget from template'), limit, h('button', { type: 'submit' }, 'Create budget'), error);
}

async function renderDashboard(familyId) {
  const d = await api.dashboard(familyId);
  app.replaceChildren(h('section', {},
    h('a', { href: '#/' }, '← Families'),
    h('h2', {}, `${d.familyType} family`),
    h('div', { class: 'card summary' },
      stat('Income', d.income), stat('Budgeted', d.totalBudgeted),
      stat('Spent', d.totalSpent), stat('Left', d.left)),
    d.budgets.map(b => budgetCard(familyId, b)),
    budgetForm(familyId)));
}

function route() {
  const match = location.hash.match(/^#\/families\/(\d+)$/);
  const page = match ? renderDashboard(match[1]) : renderFamilies();
  page.catch(err => showError(err.message));
}

window.addEventListener('hashchange', route);
route();
