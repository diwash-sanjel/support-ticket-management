import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { STATUSES, type Page, type TicketStatus, type TicketSummary } from '../api/types';
import ErrorBanner from '../components/ErrorBanner';
import StatusBadge from '../components/StatusBadge';
import { useDebounced } from '../hooks/useDebounced';

export default function TicketListPage() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<TicketStatus | ''>('');
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [data, setData] = useState<Page<TicketSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const debouncedQ = useDebounced(q, 300);

  useEffect(() => {
    let cancelled = false; // ignore responses that arrive after the filters changed
    setLoading(true);
    api.listTickets({ q: debouncedQ, status, page })
      .then((res) => { if (!cancelled) { setData(res); setError(null); } })
      .catch((e: Error) => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [debouncedQ, status, page, reloadKey]);

  const totalPages = data?.totalPages ?? 0;

  return (
    <section>
      <div className="toolbar">
        <input aria-label="Search tickets" placeholder="Search title or description…" value={q}
          onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <select aria-label="Filter by status" value={status}
          onChange={(e) => { setStatus(e.target.value as TicketStatus | ''); setPage(0); }}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
        </select>
      </div>

      <ErrorBanner message={error} onRetry={() => setReloadKey((k) => k + 1)} />

      {loading && !data && <p className="muted">Loading…</p>}

      {data && data.content.length === 0 && !error && <p className="muted">No tickets match.</p>}

      {data && data.content.length > 0 && (
        <table aria-busy={loading}>
          <thead>
            <tr><th>#</th><th>Title</th><th>Status</th><th>Priority</th><th>Assignee</th><th>Updated</th></tr>
          </thead>
          <tbody>
            {data.content.map((t) => (
              <tr key={t.id}>
                <td>{t.id}</td>
                <td><Link to={`/tickets/${t.id}`}>{t.title}</Link></td>
                <td><StatusBadge status={t.status} /></td>
                <td>{t.priority}</td>
                <td>{t.assignee ?? <span className="muted">Unassigned</span>}</td>
                <td>{new Date(t.updatedAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {data && data.totalElements > 0 && (
        <div className="pager">
          <button className="btn" disabled={page === 0 || loading} onClick={() => setPage((p) => p - 1)}>Previous</button>
          <span>Page {data.page + 1} of {totalPages} ({data.totalElements} tickets)</span>
          <button className="btn" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((p) => p + 1)}>Next</button>
        </div>
      )}
    </section>
  );
}
