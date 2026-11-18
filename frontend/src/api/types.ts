// Mirrors spec/api-contract.md
export const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'] as const;
export type TicketStatus = (typeof STATUSES)[number];

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
export type TicketPriority = (typeof PRIORITIES)[number];

export interface TicketSummary {
  id: number;
  title: string;
  status: TicketStatus;
  priority: TicketPriority;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: number;
  author: string;
  body: string;
  createdAt: string;
}

export interface TicketDetail extends TicketSummary {
  description: string;
  version: number;
  allowedTransitions: TicketStatus[];
  comments: Comment[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTicketInput {
  title: string;
  description: string;
  priority: TicketPriority;
  assignee?: string;
}

export interface UpdateTicketInput {
  title?: string;
  description?: string;
  priority?: TicketPriority;
  assignee?: string;
}

export interface FieldError {
  field: string;
  message: string;
}
