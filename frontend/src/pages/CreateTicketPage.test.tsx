import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { detail } from '../test/fixtures';
import CreateTicketPage from './CreateTicketPage';

vi.mock('../api/client', async (orig) => {
  const actual = await orig<typeof import('../api/client')>();
  return { ...actual, api: { createTicket: vi.fn() } };
});
import { api } from '../api/client';
const createTicket = vi.mocked(api.createTicket);

const renderPage = () => render(
  <MemoryRouter initialEntries={['/tickets/new']}>
    <Routes>
      <Route path="/tickets/new" element={<CreateTicketPage />} />
      <Route path="/tickets/:id" element={<p>Detail page for ticket</p>} />
    </Routes>
  </MemoryRouter>,
);

beforeEach(() => { createTicket.mockReset(); });

describe('CreateTicketPage', () => {
  it('submits the form and navigates to the new ticket', async () => {
    createTicket.mockResolvedValue(detail({ id: 42 }));
    renderPage();
    await userEvent.type(screen.getByLabelText('Title'), 'Broken login');
    await userEvent.type(screen.getByLabelText('Description'), 'Cannot sign in');
    await userEvent.selectOptions(screen.getByLabelText('Priority'), 'CRITICAL');
    await userEvent.click(screen.getByRole('button', { name: 'Create ticket' }));

    expect(await screen.findByText('Detail page for ticket')).toBeInTheDocument();
    expect(createTicket).toHaveBeenCalledWith({ title: 'Broken login', description: 'Cannot sign in', priority: 'CRITICAL' });
  });

  it('sends the assignee only when one was typed', async () => {
    createTicket.mockResolvedValue(detail());
    renderPage();
    await userEvent.type(screen.getByLabelText('Title'), 't');
    await userEvent.type(screen.getByLabelText('Description'), 'd');
    await userEvent.type(screen.getByLabelText('Assignee'), 'bob');
    await userEvent.click(screen.getByRole('button', { name: 'Create ticket' }));
    await waitFor(() => expect(createTicket).toHaveBeenCalledWith(expect.objectContaining({ assignee: 'bob' })));
  });

  it('shows backend field errors beside the right inputs and keeps what was typed', async () => {
    createTicket.mockRejectedValue(new ApiError(400, 'VALIDATION_ERROR', 'Validation failed', [
      { field: 'title', message: 'must not be blank' },
      { field: 'description', message: 'must not be blank' },
    ]));
    renderPage();
    await userEvent.type(screen.getByLabelText('Assignee'), 'bob');
    await userEvent.click(screen.getByRole('button', { name: 'Create ticket' }));

    const alerts = await screen.findAllByRole('alert');
    expect(alerts).toHaveLength(2);
    expect(screen.getByLabelText('Title')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('Title')).toHaveAccessibleDescription('must not be blank');
    expect(screen.getByLabelText('Assignee')).toHaveValue('bob');
    expect(screen.getByRole('button', { name: 'Create ticket' })).toBeEnabled();
  });

  it('puts errors that have no input in the banner (e.g. unknown field, server down)', async () => {
    createTicket.mockRejectedValue(new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Check your connection and try again.'));
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: 'Create ticket' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
  });

  it('disables the button while submitting to prevent double submit', async () => {
    let resolve!: (v: ReturnType<typeof detail>) => void;
    createTicket.mockImplementation(() => new Promise((r) => { resolve = r; }));
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: 'Create ticket' }));
    expect(await screen.findByRole('button', { name: 'Saving…' })).toBeDisabled();
    resolve(detail());
    expect(await screen.findByText('Detail page for ticket')).toBeInTheDocument();
    expect(createTicket).toHaveBeenCalledTimes(1);
  });
});
