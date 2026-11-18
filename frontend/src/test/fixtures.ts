import type { TicketDetail, TicketSummary } from '../api/types';

export const summary = (over: Partial<TicketSummary> = {}): TicketSummary => ({
  id: 1, title: 'Printer jam', status: 'OPEN', priority: 'HIGH', assignee: 'ann',
  createdAt: '2026-11-18T10:00:00Z', updatedAt: '2026-11-18T10:00:00Z', ...over,
});

export const detail = (over: Partial<TicketDetail> = {}): TicketDetail => ({
  ...summary(), description: 'Paper stuck in tray', version: 0,
  allowedTransitions: ['IN_PROGRESS', 'CANCELLED'], comments: [], ...over,
});

export const page = (content: TicketSummary[], over = {}) => ({
  content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0, ...over,
});
