const API_BASE = '/api/v2';

async function request(method, path, body) {
  const options = { method, headers: { 'Accept': 'application/json' } };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  const response = await fetch(API_BASE + path, options);
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const fieldErrors = data && data.errors ? Object.values(data.errors).join('; ') : '';
    throw new Error(fieldErrors || (data && data.detail) || `Request failed (${response.status})`);
  }
  return data;
}

const api = {
  listFamilies: () => request('GET', '/families'),
  familyTypes: () => request('GET', '/family-types'),
  registerFamily: (type) => request('POST', '/families', { type }),
  dashboard: (familyId) => request('GET', `/families/${familyId}/dashboard`),
  createBudget: (familyId, limitAmount) => request('POST', `/families/${familyId}/budgets`, { limitAmount }),
  recordTransaction: (familyId, budgetId, categoryId, form) =>
    request('POST', `/families/${familyId}/budgets/${budgetId}/categories/${categoryId}/transactions`, form)
};
