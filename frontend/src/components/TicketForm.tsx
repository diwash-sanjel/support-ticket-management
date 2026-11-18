import { useState, type FormEvent } from 'react';
import { PRIORITIES, type TicketPriority } from '../api/types';

export interface TicketFormValues {
  title: string;
  description: string;
  priority: TicketPriority;
  assignee: string;
}

interface Props {
  initial: TicketFormValues;
  submitLabel: string;
  submitting: boolean;
  disabled?: boolean;
  fieldErrors: Record<string, string>;
  onSubmit: (values: TicketFormValues) => void;
}

export const FORM_FIELDS = ['title', 'description', 'priority', 'assignee'];

export default function TicketForm({ initial, submitLabel, submitting, disabled, fieldErrors, onSubmit }: Props) {
  const [values, setValues] = useState(initial);
  const set = <K extends keyof TicketFormValues>(key: K, v: TicketFormValues[K]) =>
    setValues((prev) => ({ ...prev, [key]: v }));

  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit(values);
  };

  const err = (field: string) =>
    fieldErrors[field] ? <div className="field-error" id={`${field}-error`} role="alert">{fieldErrors[field]}</div> : null;

  return (
    <form onSubmit={submit} noValidate>
      <label htmlFor="title">Title</label>
      <input id="title" value={values.title} disabled={disabled} aria-invalid={!!fieldErrors.title}
        aria-describedby={fieldErrors.title ? 'title-error' : undefined}
        onChange={(e) => set('title', e.target.value)} />
      {err('title')}

      <label htmlFor="description">Description</label>
      <textarea id="description" value={values.description} disabled={disabled} aria-invalid={!!fieldErrors.description}
        aria-describedby={fieldErrors.description ? 'description-error' : undefined}
        onChange={(e) => set('description', e.target.value)} />
      {err('description')}

      <label htmlFor="priority">Priority</label>
      <select id="priority" value={values.priority} disabled={disabled}
        onChange={(e) => set('priority', e.target.value as TicketPriority)}>
        {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
      </select>
      {err('priority')}

      <label htmlFor="assignee">Assignee</label>
      <input id="assignee" value={values.assignee} disabled={disabled} aria-invalid={!!fieldErrors.assignee}
        aria-describedby={fieldErrors.assignee ? 'assignee-error' : undefined}
        onChange={(e) => set('assignee', e.target.value)} />
      {err('assignee')}

      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={submitting || disabled}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  );
}
