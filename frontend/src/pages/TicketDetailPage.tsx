import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import { splitError } from '../api/errors';
import type { TicketDetail, TicketStatus, UpdateTicketInput } from '../api/types';
import ErrorBanner from '../components/ErrorBanner';
import StatusBadge from '../components/StatusBadge';
import TicketForm, { FORM_FIELDS, type TicketFormValues } from '../components/TicketForm';

const COMMENT_FIELDS = ['author', 'body'];

export default function TicketDetailPage() {
  const { id } = useParams();
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loadError, setLoadError] = useState<ApiError | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [editErrors, setEditErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const [author, setAuthor] = useState('');
  const [body, setBody] = useState('');
  const [commentErrors, setCommentErrors] = useState<Record<string, string>>({});
  const [commenting, setCommenting] = useState(false);

  const load = useCallback(async () => {
    try {
      setTicket(await api.getTicket(id!));
      setLoadError(null);
    } catch (e) {
      setLoadError(e instanceof ApiError ? e : new ApiError(0, 'UNKNOWN_ERROR', 'Something went wrong.'));
    }
  }, [id]);

  useEffect(() => { void load(); }, [load]);

  // A malformed id (e.g. /tickets/abc) is rejected with 400; to the user that is simply a ticket that does not exist.
  if (loadError?.code === 'TICKET_NOT_FOUND' || loadError?.status === 400) {
    return (
      <section className="card">
        <h2>Ticket not found</h2>
        <p>{loadError.status === 400 ? `There is no ticket "${id}".` : loadError.message}</p>
        <Link to="/">Back to tickets</Link>
      </section>
    );
  }
  if (loadError && !ticket) {
    return <ErrorBanner message={loadError.message} onRetry={() => void load()} />;
  }
  if (!ticket) return <p className="muted">Loading…</p>;

  const terminal = ticket.allowedTransitions.length === 0;

  const doTransition = async (target: TicketStatus) => {
    setBusy(true);
    setActionError(null);
    setNotice(null);
    try {
      setTicket(await api.transition(ticket.id, target));
    } catch (e) {
      setActionError(splitError(e, []).general);
      await load(); // server state is the truth: refresh so buttons match reality
    } finally {
      setBusy(false);
    }
  };

  const save = async (v: TicketFormValues) => {
    const changes: UpdateTicketInput = {};
    if (v.title !== ticket.title) changes.title = v.title;
    if (v.description !== ticket.description) changes.description = v.description;
    if (v.priority !== ticket.priority) changes.priority = v.priority;
    if (v.assignee !== (ticket.assignee ?? '')) changes.assignee = v.assignee; // '' clears the assignee
    if (Object.keys(changes).length === 0) {
      setNotice('No changes to save.');
      return;
    }
    setSaving(true);
    setEditErrors({});
    setActionError(null);
    setNotice(null);
    try {
      setTicket(await api.updateTicket(ticket.id, changes));
      setNotice('Ticket updated.');
    } catch (e) {
      const s = splitError(e, FORM_FIELDS);
      setEditErrors(s.fields);
      setActionError(s.general);
      if (e instanceof ApiError && e.status === 409) await load();
    } finally {
      setSaving(false);
    }
  };

  const submitComment = async (e: FormEvent) => {
    e.preventDefault();
    setCommenting(true);
    setCommentErrors({});
    setActionError(null);
    try {
      const created = await api.addComment(ticket.id, { author, body });
      setTicket({ ...ticket, comments: [...ticket.comments, created] });
      setBody('');
    } catch (err) {
      const s = splitError(err, COMMENT_FIELDS);
      setCommentErrors(s.fields);
      setActionError(s.general);
    } finally {
      setCommenting(false);
    }
  };

  return (
    <section>
      <p><Link to="/">← All tickets</Link></p>
      <ErrorBanner message={actionError} onDismiss={() => setActionError(null)} />
      {notice && <p role="status" className="muted">{notice}</p>}

      <div className="card">
        <h2>#{ticket.id} {ticket.title} <StatusBadge status={ticket.status} /></h2>
        <p className="meta">
          Priority {ticket.priority} · {ticket.assignee ? `Assigned to ${ticket.assignee}` : 'Unassigned'} ·
          Updated {new Date(ticket.updatedAt).toLocaleString()}
        </p>
        <p className="pre">{ticket.description}</p>

        <div className="actions" aria-label="Status actions">
          {ticket.allowedTransitions.map((t) => (
            <button key={t} className={`btn ${t === 'CANCELLED' ? 'btn-danger' : 'btn-primary'}`}
              disabled={busy} onClick={() => void doTransition(t)}>
              {t === 'CANCELLED' ? 'Cancel ticket' : `Move to ${t.replace('_', ' ')}`}
            </button>
          ))}
          {terminal && <span className="muted">No further status changes are possible.</span>}
        </div>
      </div>

      <div className="card">
        <h3>Edit details</h3>
        {terminal && <p className="muted">This ticket is {ticket.status} and can no longer be edited.</p>}
        <TicketForm
          // Remount only when the saved field values change, so a status change does not wipe unsaved typing.
          key={`${ticket.title}\u0000${ticket.description}\u0000${ticket.priority}\u0000${ticket.assignee ?? ''}`}
          initial={{ title: ticket.title, description: ticket.description, priority: ticket.priority, assignee: ticket.assignee ?? '' }}
          submitLabel="Save changes" submitting={saving} disabled={terminal}
          fieldErrors={editErrors} onSubmit={(v) => void save(v)} />
      </div>

      <div className="card">
        <h3>Comments ({ticket.comments.length})</h3>
        {ticket.comments.length === 0 && <p className="muted">No comments yet.</p>}
        {ticket.comments.map((c) => (
          <div className="comment" key={c.id}>
            <div className="meta"><strong>{c.author}</strong> · {new Date(c.createdAt).toLocaleString()}</div>
            <div className="pre">{c.body}</div>
          </div>
        ))}
        <form onSubmit={(e) => void submitComment(e)} noValidate>
          <label htmlFor="author">Your name</label>
          <input id="author" value={author} aria-invalid={!!commentErrors.author} onChange={(e) => setAuthor(e.target.value)} />
          {commentErrors.author && <div className="field-error" role="alert">{commentErrors.author}</div>}
          <label htmlFor="body">Comment</label>
          <textarea id="body" value={body} aria-invalid={!!commentErrors.body} onChange={(e) => setBody(e.target.value)} />
          {commentErrors.body && <div className="field-error" role="alert">{commentErrors.body}</div>}
          <div className="actions">
            <button type="submit" className="btn btn-primary" disabled={commenting}>
              {commenting ? 'Posting…' : 'Add comment'}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
