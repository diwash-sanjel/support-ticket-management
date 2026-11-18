import { ApiError } from './client';

export interface SplitError {
  /** field name -> message, for fields this form actually shows */
  fields: Record<string, string>;
  /** anything that cannot be shown beside an input: goes in the banner */
  general: string | null;
}

/** Splits an error into inline field messages and a general banner message. */
export function splitError(err: unknown, knownFields: string[]): SplitError {
  if (!(err instanceof ApiError)) {
    return { fields: {}, general: 'Something went wrong. Please try again.' };
  }
  const fields: Record<string, string> = {};
  const unplaced: string[] = [];
  for (const fe of err.fieldErrors) {
    if (knownFields.includes(fe.field)) fields[fe.field] = fe.message;
    else unplaced.push(`${fe.field}: ${fe.message}`);
  }
  if (err.code === 'VALIDATION_ERROR' && err.fieldErrors.length > 0) {
    return { fields, general: unplaced.length ? unplaced.join('; ') : null };
  }
  return { fields, general: err.message };
}
