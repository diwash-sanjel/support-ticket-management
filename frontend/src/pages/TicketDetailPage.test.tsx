import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { detail } from '../test/fixtures';
import TicketDetailPage from './TicketDetailPage';

vi.mock('../api/client', async (orig) => {
  const actual = await orig<typeof import('../api/client')>();
  return { ...actual, api: { getTicket: vi.fn(), transition: vi.fn(), updateTicket: vi.fn(), addComment: vi.fn() } };
});
import { api } from '../api/client';
const m = vi.mocked(api);

const renderPage = () => render(
  <MemoryRouter initialEntries={['/tickets/1']}>
    <Routes><Route path="/tickets/:id" element={<TicketDetailPage />} /></Routes>
  </MemoryRouter>,
);

beforeEach(() => { Object.values(m).forEach((f) => f.mockReset()); });

describe('status transitions', () => {
  it('renders one button per allowed transition, taken from the API', async () => {
    m.getTicket.mockResolvedValue(detail({ status: 'IN_PROGRESS', allowedTransitions: ['RESOLVED', 'CANCELLED'] }));
    renderPage();
    const actions = within(await screen.findByLabelText('Status actions'));
    expect(actions.getAllByRole('button').map((b) => b.textContent)).toEqual(['Move to RESOLVED', 'Cancel ticket']);
  });

  it('moves the ticket and re-renders buttons from the response', async () => {
    m.getTicket.mockResolvedValue(detail());
    m.transition.mockResolvedValue(detail({ status: 'IN_PROGRESS', allowedTransitions: ['RESOLVED', 'CANCELLED'] }));
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: 'Move to IN PROGRESS' }));
    expect(m.transition).toHaveBeenCalledWith(1, 'IN_PROGRESS');
    expect(await screen.findByRole('button', { name: 'Move to RESOLVED' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Move to IN PROGRESS' })).not.toBeInTheDocument();
  });

  it('shows the server message on a rejected transition and refreshes the ticket', async () => {
    m.getTicket.mockResolvedValueOnce(detail());
    m.transition.mockRejectedValue(new ApiError(409, 'INVALID_STATUS_TRANSITION',
      'Cannot transition from IN_PROGRESS to IN_PROGRESS. Allowed: RESOLVED, CANCELLED'));
    m.getTicket.mockResolvedValue(detail({ status: 'IN_PROGRESS', allowedTransitions: ['RESOLVED', 'CANCELLED'] }));
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: 'Move to IN PROGRESS' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Allowed: RESOLVED, CANCELLED');
    expect(await screen.findByRole('button', { name: 'Move to RESOLVED' })).toBeInTheDocument();
  });

  it('terminal ticket: no action buttons, edit form disabled, explanation shown', async () => {
    m.getTicket.mockResolvedValue(detail({ status: 'CLOSED', allowedTransitions: [] }));
    renderPage();
    expect(await screen.findByText('No further status changes are possible.')).toBeInTheDocument();
    expect(within(screen.getByLabelText('Status actions')).queryAllByRole('button')).toHaveLength(0);
    expect(screen.getByText(/can no longer be edited/)).toBeInTheDocument();
    expect(screen.getByLabelText('Title')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Add comment' })).toBeEnabled(); // comments stay allowed (A3)
  });
});

describe('loading and not found', () => {
  it('shows a not-found page with a link back', async () => {
    m.getTicket.mockRejectedValue(new ApiError(404, 'TICKET_NOT_FOUND', 'Ticket 9 not found'));
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Ticket not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to tickets' })).toHaveAttribute('href', '/');
  });

  it('shows a retryable error when the server is unreachable', async () => {
    m.getTicket.mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server.'));
    m.getTicket.mockResolvedValue(detail());
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('heading', { name: /Printer jam/ })).toBeInTheDocument();
  });
});

describe('review fixes', () => {
  it('a malformed ticket id (backend 400) shows the not-found page, not a retry banner', async () => {
    m.getTicket.mockRejectedValue(new ApiError(400, 'VALIDATION_ERROR', 'Validation failed',
      [{ field: 'id', message: "Invalid value 'abc'" }]));
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Ticket not found' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
  });

  it('unsaved edits survive a status change', async () => {
    m.getTicket.mockResolvedValue(detail());
    m.transition.mockResolvedValue(detail({ status: 'IN_PROGRESS', allowedTransitions: ['RESOLVED', 'CANCELLED'], version: 1 }));
    renderPage();
    const title = await screen.findByLabelText('Title');
    await userEvent.clear(title);
    await userEvent.type(title, 'Half-typed edit');
    await userEvent.click(screen.getByRole('button', { name: 'Move to IN PROGRESS' }));
    await screen.findByRole('button', { name: 'Move to RESOLVED' });
    expect(screen.getByLabelText('Title')).toHaveValue('Half-typed edit');
  });

  it('after a successful save the form shows the saved values', async () => {
    m.getTicket.mockResolvedValue(detail());
    m.updateTicket.mockResolvedValue(detail({ title: 'Saved title', version: 1 }));
    renderPage();
    const title = await screen.findByLabelText('Title');
    await userEvent.clear(title);
    await userEvent.type(title, 'Saved title');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await screen.findByText('Ticket updated.');
    expect(screen.getByLabelText('Title')).toHaveValue('Saved title');
  });
});

describe('editing', () => {
  it('sends only the changed fields', async () => {
    m.getTicket.mockResolvedValue(detail());
    m.updateTicket.mockResolvedValue(detail({ title: 'New title', version: 1 }));
    renderPage();
    const title = await screen.findByLabelText('Title');
    await userEvent.clear(title);
    await userEvent.type(title, 'New title');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(m.updateTicket).toHaveBeenCalledWith(1, { title: 'New title' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Ticket updated.');
  });

  it('clearing the assignee sends an empty string (which the backend treats as "clear")', async () => {
    m.getTicket.mockResolvedValue(detail({ assignee: 'ann' }));
    m.updateTicket.mockResolvedValue(detail({ assignee: null, version: 1 }));
    renderPage();
    await userEvent.clear(await screen.findByLabelText('Assignee'));
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(m.updateTicket).toHaveBeenCalledWith(1, { assignee: '' }));
    expect(await screen.findByText(/Unassigned/)).toBeInTheDocument();
  });

  it('changes the assignee and priority', async () => {
    m.getTicket.mockResolvedValue(detail());
    m.updateTicket.mockResolvedValue(detail({ assignee: 'carol', priority: 'LOW', version: 1 }));
    renderPage();
    const assignee = await screen.findByLabelText('Assignee');
    await userEvent.clear(assignee);
    await userEvent.type(assignee, 'carol');
    await userEvent.selectOptions(screen.getByLabelText('Priority'), 'LOW');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(m.updateTicket).toHaveBeenCalledWith(1, { priority: 'LOW', assignee: 'carol' }));
    expect(await screen.findByText(/Assigned to carol/)).toBeInTheDocument();
  });

  it('does not call the API when nothing changed', async () => {
    m.getTicket.mockResolvedValue(detail());
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: 'Save changes' }));
    expect(m.updateTicket).not.toHaveBeenCalled();
    expect(await screen.findByRole('status')).toHaveTextContent('No changes to save.');
  });

  it('shows backend validation errors beside the field', async () => {
    m.getTicket.mockResolvedValue(detail());
    m.updateTicket.mockRejectedValue(new ApiError(400, 'VALIDATION_ERROR', 'Validation failed',
      [{ field: 'title', message: 'must not be blank' }]));
    renderPage();
    await userEvent.clear(await screen.findByLabelText('Title'));
    await userEvent.type(screen.getByLabelText('Title'), ' ');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(await screen.findByText('must not be blank')).toBeInTheDocument();
    expect(screen.getByLabelText('Title')).toHaveAttribute('aria-invalid', 'true');
  });

  it('a 409 on save (e.g. ticket got closed elsewhere) shows the message and reloads', async () => {
    m.getTicket.mockResolvedValueOnce(detail());
    m.updateTicket.mockRejectedValue(new ApiError(409, 'TICKET_NOT_EDITABLE', 'Ticket 1 is CLOSED and can no longer be edited'));
    m.getTicket.mockResolvedValue(detail({ status: 'CLOSED', allowedTransitions: [], version: 3 }));
    renderPage();
    const title = await screen.findByLabelText('Title');
    await userEvent.type(title, '!');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('can no longer be edited');
    await waitFor(() => expect(screen.getByLabelText('Title')).toBeDisabled());
  });
});

describe('comments', () => {
  it('lists existing comments and appends a new one, clearing only the body', async () => {
    m.getTicket.mockResolvedValue(detail({ comments: [{ id: 1, author: 'Ann', body: 'First', createdAt: '2026-11-18T10:00:00Z' }] }));
    m.addComment.mockResolvedValue({ id: 2, author: 'Bob', body: 'Second', createdAt: '2026-11-18T11:00:00Z' });
    renderPage();
    expect(await screen.findByText('First')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Your name'), 'Bob');
    await userEvent.type(screen.getByLabelText('Comment'), 'Second');
    await userEvent.click(screen.getByRole('button', { name: 'Add comment' }));

    expect(await screen.findByText('Second')).toBeInTheDocument();
    expect(m.addComment).toHaveBeenCalledWith(1, { author: 'Bob', body: 'Second' });
    expect(screen.getByLabelText('Comment')).toHaveValue('');
    expect(screen.getByLabelText('Your name')).toHaveValue('Bob');
    expect(screen.getByText('Comments (2)')).toBeInTheDocument();
  });

  it('shows inline errors for a blank author/body and keeps the form usable', async () => {
    m.getTicket.mockResolvedValue(detail());
    m.addComment.mockRejectedValue(new ApiError(400, 'VALIDATION_ERROR', 'Validation failed', [
      { field: 'author', message: 'must not be blank' }, { field: 'body', message: 'must not be blank' }]));
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: 'Add comment' }));
    expect(await screen.findAllByText('must not be blank')).toHaveLength(2);
    expect(screen.getByText('No comments yet.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add comment' })).toBeEnabled();
  });
});
