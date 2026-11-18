import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from './client';

function mockFetch(status: number, body: unknown, rawText?: string) {
  const fn = vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      if (rawText !== undefined) throw new SyntaxError('not json');
      return body;
    },
  });
  vi.stubGlobal('fetch', fn);
  return fn;
}

afterEach(() => vi.unstubAllGlobals());

describe('api client error handling', () => {
  it('turns a structured backend error into ApiError with code, message and field errors', async () => {
    mockFetch(400, {
      status: 400, code: 'VALIDATION_ERROR', message: 'Validation failed',
      fieldErrors: [{ field: 'title', message: 'must not be blank' }],
    });
    const err = await api.createTicket({ title: '', description: 'd', priority: 'LOW' }).catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(400);
    expect(err.code).toBe('VALIDATION_ERROR');
    expect(err.fieldMessage('title')).toBe('must not be blank');
    expect(err.fieldMessage('description')).toBeUndefined();
  });

  it('keeps the server message for a rejected transition', async () => {
    mockFetch(409, {
      status: 409, code: 'INVALID_STATUS_TRANSITION',
      message: 'Cannot transition from OPEN to CLOSED. Allowed: IN_PROGRESS, CANCELLED',
    });
    const err = await api.transition(1, 'CLOSED').catch((e) => e);
    expect(err.code).toBe('INVALID_STATUS_TRANSITION');
    expect(err.message).toContain('Allowed: IN_PROGRESS, CANCELLED');
    expect(err.fieldErrors).toEqual([]);
  });

  it('maps a network failure to a friendly NETWORK_ERROR', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    const err = await api.listTickets().catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.isNetworkError).toBe(true);
    expect(err.message).toMatch(/cannot reach the server/i);
  });

  it('handles a non-JSON error body (e.g. proxy 502) without crashing', async () => {
    mockFetch(502, null, 'Bad Gateway');
    const err = await api.getTicket(1).catch((e) => e);
    expect(err.code).toBe('UNKNOWN_ERROR');
    expect(err.message).toContain('502');
  });

  it('handles a JSON body that is not our error shape', async () => {
    mockFetch(500, { oops: true });
    const err = await api.getTicket(1).catch((e) => e);
    expect(err.code).toBe('UNKNOWN_ERROR');
  });
});

describe('api client requests', () => {
  it('builds the list query: trims q, omits empty status, includes paging', async () => {
    const fn = mockFetch(200, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    await api.listTickets({ q: '  printer ', status: '', page: 2 });
    expect(fn.mock.calls[0][0]).toBe('/api/tickets?q=printer&page=2&size=20');
    await api.listTickets({ status: 'OPEN' });
    expect(fn.mock.calls[1][0]).toBe('/api/tickets?status=OPEN&page=0&size=20');
  });

  it('omits a blank q entirely', async () => {
    const fn = mockFetch(200, { content: [] });
    await api.listTickets({ q: '   ' });
    expect(fn.mock.calls[0][0]).not.toContain('q=');
  });

  it('sends JSON bodies with the right method and path', async () => {
    const fn = mockFetch(200, {});
    await api.transition(7, 'IN_PROGRESS');
    expect(fn.mock.calls[0][0]).toBe('/api/tickets/7/transitions');
    expect(fn.mock.calls[0][1].method).toBe('POST');
    expect(JSON.parse(fn.mock.calls[0][1].body)).toEqual({ targetStatus: 'IN_PROGRESS' });
    await api.updateTicket(7, { assignee: '' });
    expect(fn.mock.calls[1][1].method).toBe('PATCH');
    expect(JSON.parse(fn.mock.calls[1][1].body)).toEqual({ assignee: '' });
  });
});
