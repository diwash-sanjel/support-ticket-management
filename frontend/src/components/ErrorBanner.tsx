interface Props {
  message: string | null;
  onDismiss?: () => void;
  onRetry?: () => void;
}

export default function ErrorBanner({ message, onDismiss, onRetry }: Props) {
  if (!message) return null;
  return (
    <div className="banner" role="alert">
      <span>{message}</span>
      <span>
        {onRetry && <button type="button" onClick={onRetry}>Retry</button>}
        {onDismiss && <button type="button" aria-label="Dismiss" onClick={onDismiss}>×</button>}
      </span>
    </div>
  );
}
