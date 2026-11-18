import type {
  CreateTicketInput, Comment, FieldError, Page, TicketDetail, TicketStatus, TicketSummary, UpdateTicketInput,
} from './types';

/** Every failure the UI can see is an ApiError, so components handle one shape. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly fieldErrors: FieldError[] = [],
  ) {
    super(message);
    this.name = 'ApiError';
  }

  /** Message for a given form field, if the server reported one. */
  fieldMessage(field: string): string | undefined {
    return this.fieldErrors.find((f) => f.field === field)?.message;
  }

  get isNetworkError(): boolean {
    return this.code === 'NETWORK_ERROR';
  }
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body = await res.json();
    if (body && typeof body.code === 'string' && typeof body.message === 'string') {
      return new ApiError(res.status, body.code, body.message, Array.isArray(body.fieldErrors) ? body.fieldErrors : []);
    }
  } catch {
    // fall through: not our JSON error body (e.g. proxy error page)
  }
  return new ApiError(res.status, 'UNKNOWN_ERROR', `Unexpected response from server (HTTP ${res.status})`);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, {
      ...init,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...init?.headers },
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Check your connection and try again.');
  }
  if (!res.ok) {
    throw await toApiError(res);
  }
  return (await res.json()) as T;
}

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) });

export interface ListParams {
  q?: string;
  status?: TicketStatus | '';
  page?: number;
  size?: number;
}

export const api = {
  listTickets(params: ListParams = {}): Promise<Page<TicketSummary>> {
    const qs = new URLSearchParams();
    if (params.q?.trim()) qs.set('q', params.q.trim());
    if (params.status) qs.set('status', params.status);
    qs.set('page', String(params.page ?? 0));
    qs.set('size', String(params.size ?? 20));
    return request(`/api/tickets?${qs}`);
  },
  getTicket: (id: number | string): Promise<TicketDetail> => request(`/api/tickets/${id}`),
  createTicket: (input: CreateTicketInput): Promise<TicketDetail> => request('/api/tickets', json('POST', input)),
  updateTicket: (id: number, input: UpdateTicketInput): Promise<TicketDetail> =>
    request(`/api/tickets/${id}`, json('PATCH', input)),
  transition: (id: number, targetStatus: TicketStatus): Promise<TicketDetail> =>
    request(`/api/tickets/${id}/transitions`, json('POST', { targetStatus })),
  addComment: (id: number, input: { author: string; body: string }): Promise<Comment> =>
    request(`/api/tickets/${id}/comments`, json('POST', input)),
};
