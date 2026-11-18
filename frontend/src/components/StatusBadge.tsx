import type { TicketStatus } from '../api/types';

export default function StatusBadge({ status }: { status: TicketStatus }) {
  return <span className={`badge badge-${status}`}>{status.replace('_', ' ')}</span>;
}
