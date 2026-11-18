import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

/** Unique per run so the tests do not depend on what is already in the database. */
const uid = () => `${Date.now().toString(36)}${Math.floor(Math.random() * 1e4)}`;

async function createViaUi(page: Page, title: string, description = 'E2E description', priority = 'MEDIUM', assignee = '') {
  await page.goto('/tickets/new');
  await page.getByLabel('Title').fill(title);
  await page.getByLabel('Description').fill(description);
  await page.getByLabel('Priority').selectOption(priority);
  if (assignee) await page.getByLabel('Assignee').fill(assignee);
  await page.getByRole('button', { name: 'Create ticket' }).click();
  await expect(page).toHaveURL(/\/tickets\/\d+$/);
  return Number(page.url().split('/').pop());
}

async function apiTransition(request: APIRequestContext, id: number, targetStatus: string) {
  const res = await request.post(`/api/tickets/${id}/transitions`, { data: { targetStatus } });
  expect(res.ok()).toBeTruthy();
}

test('AC1/3: create a ticket from the UI and see its details', async ({ page }) => {
  const title = `Create ${uid()}`;
  await createViaUi(page, title, 'Printer is jammed', 'HIGH', 'ann');
  await expect(page.getByRole('heading', { name: new RegExp(title) })).toBeVisible();
  await expect(page.getByText('Printer is jammed').first()).toBeVisible();
  await expect(page.getByText(/Priority HIGH · Assigned to ann/)).toBeVisible();
  await expect(page.locator('h2 .badge')).toHaveText('OPEN');
});

test('AC12/13: backend validation errors appear beside the fields', async ({ page }) => {
  await page.goto('/tickets/new');
  await page.getByRole('button', { name: 'Create ticket' }).click();
  await expect(page.locator('#title-error')).toHaveText('must not be blank');
  await expect(page.locator('#description-error')).toHaveText('must not be blank');
  await expect(page.getByLabel('Title')).toHaveAttribute('aria-invalid', 'true');
  await expect(page).toHaveURL(/\/tickets\/new$/);
});

test('AC2/7/8: list, search (case-insensitive, literal %), status filter, empty state', async ({ page, request }) => {
  const tag = uid();
  const a = await request.post('/api/tickets', { data: { title: `Alpha ${tag}`, description: 'first thing' } });
  const b = await request.post('/api/tickets', { data: { title: `Beta ${tag} 100% off`, description: 'second thing' } });
  const idB = (await b.json()).id;
  await apiTransition(request, idB, 'IN_PROGRESS');
  expect(a.ok()).toBeTruthy();

  await page.goto('/');
  await page.getByLabel('Search tickets').fill(tag.toUpperCase());
  await expect(page.getByRole('link', { name: `Alpha ${tag}` })).toBeVisible();
  await expect(page.getByRole('link', { name: new RegExp(`Beta ${tag}`) })).toBeVisible();

  await page.getByLabel('Filter by status').selectOption('IN_PROGRESS');
  await expect(page.getByRole('link', { name: `Alpha ${tag}` })).toHaveCount(0);
  await expect(page.getByRole('link', { name: new RegExp(`Beta ${tag}`) })).toBeVisible();

  await page.getByLabel('Filter by status').selectOption('');
  await page.getByLabel('Search tickets').fill(`${tag} 100%`);
  await expect(page.getByRole('link', { name: new RegExp(`Beta ${tag}`) })).toBeVisible();
  await expect(page.getByRole('link', { name: `Alpha ${tag}` })).toHaveCount(0);

  await page.getByLabel('Search tickets').fill(`nomatch-${tag}`);
  await expect(page.getByText('No tickets match.')).toBeVisible();
});

test('AC4/5: edit fields, change then clear the assignee', async ({ page }) => {
  const title = `Edit ${uid()}`;
  await createViaUi(page, title);
  await page.getByLabel('Title').fill(`${title} renamed`);
  await page.getByLabel('Priority').selectOption('CRITICAL');
  await page.getByLabel('Assignee').fill('carol');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.getByRole('status')).toHaveText('Ticket updated.');
  await expect(page.getByRole('heading', { name: new RegExp(`${title} renamed`) })).toBeVisible();
  await expect(page.getByText(/Priority CRITICAL · Assigned to carol/)).toBeVisible();

  await page.getByLabel('Assignee').fill('');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.getByText(/Priority CRITICAL · Unassigned/)).toBeVisible();

  await page.reload(); // really persisted, not just client state
  await expect(page.getByLabel('Assignee')).toHaveValue('');
  await expect(page.getByLabel('Title')).toHaveValue(`${title} renamed`);
});

test('AC6: add comments', async ({ page }) => {
  await createViaUi(page, `Comments ${uid()}`);
  await page.getByLabel('Your name').fill('Bob');
  await page.getByLabel('Comment', { exact: true }).fill('Looking into it');
  await page.getByRole('button', { name: 'Add comment' }).click();
  await expect(page.getByText('Looking into it')).toBeVisible();
  await expect(page.getByText('Comments (1)')).toBeVisible();

  await page.getByRole('button', { name: 'Add comment' }).click(); // blank body now
  await expect(page.getByText('must not be blank')).toBeVisible();

  await page.reload();
  await expect(page.getByText('Looking into it')).toBeVisible();
});

test('AC9: walk OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED; closed ticket is frozen but commentable', async ({ page }) => {
  await createViaUi(page, `Lifecycle ${uid()}`);
  const status = page.locator('h2 .badge');
  await expect(status).toHaveText('OPEN');
  await expect(page.getByLabel('Status actions').getByRole('button')).toHaveText(['Move to IN PROGRESS', 'Cancel ticket']);

  await page.getByRole('button', { name: 'Move to IN PROGRESS' }).click();
  await expect(status).toHaveText('IN PROGRESS');
  await page.getByRole('button', { name: 'Move to RESOLVED' }).click();
  await expect(status).toHaveText('RESOLVED');
  await expect(page.getByLabel('Status actions').getByRole('button')).toHaveText(['Move to CLOSED']);
  await page.getByRole('button', { name: 'Move to CLOSED' }).click();
  await expect(status).toHaveText('CLOSED');

  await expect(page.getByLabel('Status actions').getByRole('button')).toHaveCount(0);
  await expect(page.getByText('No further status changes are possible.')).toBeVisible();
  await expect(page.getByLabel('Title')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Save changes' })).toBeDisabled();

  await page.getByLabel('Your name').fill('Ann');
  await page.getByLabel('Comment', { exact: true }).fill('Post-mortem note');
  await page.getByRole('button', { name: 'Add comment' }).click();
  await expect(page.getByText('Post-mortem note')).toBeVisible();
});

test('AC10/13: a stale page cannot force an invalid transition; server message shown and UI refreshes', async ({ page, request }) => {
  const id = await createViaUi(page, `Stale ${uid()}`);
  // The page must have *loaded* the OPEN state first, otherwise it would simply load the newer state and
  // there would be nothing stale to test.
  await expect(page.getByRole('button', { name: 'Move to IN PROGRESS' })).toBeVisible();
  // Someone else moves the ticket on while this page still shows the OPEN buttons.
  await apiTransition(request, id, 'IN_PROGRESS');
  await apiTransition(request, id, 'RESOLVED');

  await page.getByRole('button', { name: 'Move to IN PROGRESS' }).click(); // stale button
  await expect(page.getByRole('alert')).toContainText('Cannot transition from RESOLVED to IN_PROGRESS. Allowed: CLOSED');
  await expect(page.locator('h2 .badge')).toHaveText('RESOLVED');           // UI refreshed from the server
  await expect(page.getByRole('button', { name: 'Move to CLOSED' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Move to IN PROGRESS' })).toHaveCount(0);
});

test('AC10: backend itself rejects forbidden transitions (not just the UI)', async ({ request }) => {
  const created = await request.post('/api/tickets', { data: { title: `Direct ${uid()}`, description: 'd' } });
  const id = (await created.json()).id;
  const bad = await request.post(`/api/tickets/${id}/transitions`, { data: { targetStatus: 'CLOSED' } });
  expect(bad.status()).toBe(409);
  expect((await bad.json()).code).toBe('INVALID_STATUS_TRANSITION');
  await apiTransition(request, id, 'CANCELLED');
  const reopen = await request.post(`/api/tickets/${id}/transitions`, { data: { targetStatus: 'OPEN' } });
  expect(reopen.status()).toBe(409);
});

test('AC13: unknown ticket shows a not-found page', async ({ page }) => {
  await page.goto('/tickets/99999999');
  await expect(page.getByRole('heading', { name: 'Ticket not found' })).toBeVisible();
  await page.getByRole('link', { name: 'Back to tickets' }).click();
  await expect(page).toHaveURL(/\/$/);
});

test('AC13: a malformed ticket URL shows not-found, not a confusing validation banner', async ({ page }) => {
  await page.goto('/tickets/abc');
  await expect(page.getByRole('heading', { name: 'Ticket not found' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Retry' })).toHaveCount(0);
});

test('unsaved edits survive a status change (real UI)', async ({ page }) => {
  await createViaUi(page, `Keep edits ${uid()}`);
  await page.getByLabel('Title').fill('typed but not saved');
  await page.getByRole('button', { name: 'Move to IN PROGRESS' }).click();
  await expect(page.locator('h2 .badge')).toHaveText('IN PROGRESS');
  await expect(page.getByLabel('Title')).toHaveValue('typed but not saved');
});

test('AC13: server unreachable shows a friendly retryable error, then recovers', async ({ page }) => {
  let down = true;
  await page.route('**/api/tickets?*', (route) => (down ? route.abort('connectionrefused') : route.continue()));
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('Cannot reach the server');
  down = false;
  await page.getByRole('button', { name: 'Retry' }).click();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await expect(page.getByLabel('Search tickets')).toBeVisible();
});
