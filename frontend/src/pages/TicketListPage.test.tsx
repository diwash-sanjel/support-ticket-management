import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { page, summary } from '../test/fixtures';
import TicketListPage from './TicketListPage';

vi.mock('../api/client', async (orig) => {
  const actual = await orig<typeof import('../api/client')>();
  return { ...actual, api: { listTickets: vi.fn() } };
});
import { api } from '../api/client';
const listTickets = vi.mocked(api.listTickets);

const renderPage = () => render(<MemoryRouter><TicketListPage /></MemoryRouter>);

beforeEach(() => { listTickets.mockReset(); });

describe('TicketListPage', () => {
  it('lists tickets with status, priority and assignee', async () => {
    listTickets.mockResolvedValue(page([
      summary({ id: 1, title: 'Printer jam' }),
      summary({ id: 2, title: 'Login error', status: 'IN_PROGRESS', assignee: null }),
    ]));
    renderPage();
    expect(await screen.findByRole('link', { name: 'Printer jam' })).toHaveAttribute('href', '/tickets/1');
    expect(within(screen.getByRole('table')).getByText('IN PROGRESS')).toBeInTheDocument();
    expect(screen.getByText('Unassigned')).toBeInTheDocument();
  });

  it('shows an empty state when nothing matches', async () => {
    listTickets.mockResolvedValue(page([]));
    renderPage();
    expect(await screen.findByText('No tickets match.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('searches after the debounce, once, with the trimmed text and page 0', async () => {
    listTickets.mockResolvedValue(page([summary()]));
    renderPage();
    await screen.findByRole('table');
    listTickets.mockClear();

    await userEvent.type(screen.getByLabelText('Search tickets'), 'prin');
    expect(listTickets).not.toHaveBeenCalled(); // still debouncing
    await waitFor(() => expect(listTickets).toHaveBeenCalledTimes(1));
    expect(listTickets).toHaveBeenLastCalledWith({ q: 'prin', status: '', page: 0 });
  });

  it('filters by status and resets to the first page', async () => {
    listTickets.mockResolvedValue(page([summary()], { totalElements: 45, totalPages: 3 }));
    renderPage();
    await screen.findByRole('table');
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(listTickets).toHaveBeenLastCalledWith({ q: '', status: '', page: 1 }));

    await userEvent.selectOptions(screen.getByLabelText('Filter by status'), 'CLOSED');
    await waitFor(() => expect(listTickets).toHaveBeenLastCalledWith({ q: '', status: 'CLOSED', page: 0 }));
  });

  it('pagination buttons are disabled at the ends', async () => {
    listTickets.mockResolvedValue(page([summary()], { totalElements: 45, totalPages: 3 }));
    renderPage();
    await screen.findByText(/Page 1 of 3/);
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();
  });

  it('shows the error with a retry that reloads', async () => {
    listTickets.mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Check your connection and try again.'));
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');

    listTickets.mockResolvedValue(page([summary({ title: 'Recovered' })]));
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('link', { name: 'Recovered' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('ignores a slow response that arrives after a newer one (no stale results)', async () => {
    let resolveSlow!: (v: ReturnType<typeof page>) => void;
    listTickets.mockImplementationOnce(() => new Promise((r) => { resolveSlow = r; }));
    listTickets.mockResolvedValue(page([summary({ title: 'Fresh result' })]));
    renderPage();                                   // first request: slow, still pending
    await userEvent.selectOptions(screen.getByLabelText('Filter by status'), 'OPEN'); // second: fast
    expect(await screen.findByRole('link', { name: 'Fresh result' })).toBeInTheDocument();

    resolveSlow(page([summary({ title: 'Stale result' })]));
    await new Promise((r) => setTimeout(r, 20));
    expect(screen.queryByText('Stale result')).not.toBeInTheDocument();
    expect(screen.getByText('Fresh result')).toBeInTheDocument();
  });
});
