import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { splitError } from '../api/errors';
import ErrorBanner from '../components/ErrorBanner';
import TicketForm, { FORM_FIELDS, type TicketFormValues } from '../components/TicketForm';

const EMPTY: TicketFormValues = { title: '', description: '', priority: 'MEDIUM', assignee: '' };

export default function CreateTicketPage() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);

  const submit = async (v: TicketFormValues) => {
    setSubmitting(true);
    setFieldErrors({});
    setGeneral(null);
    try {
      const created = await api.createTicket({
        title: v.title, description: v.description, priority: v.priority,
        ...(v.assignee.trim() ? { assignee: v.assignee } : {}),
      });
      navigate(`/tickets/${created.id}`);
    } catch (e) {
      const s = splitError(e, FORM_FIELDS);
      setFieldErrors(s.fields);
      setGeneral(s.general);
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <h2>New ticket</h2>
      <ErrorBanner message={general} onDismiss={() => setGeneral(null)} />
      <TicketForm initial={EMPTY} submitLabel="Create ticket" submitting={submitting}
        fieldErrors={fieldErrors} onSubmit={submit} />
      <p><Link to="/">Cancel</Link></p>
    </section>
  );
}
