import { DOCS_URL } from './site';

export interface ApiErrorBody {
  error: {
    code: string;
    documentation: string;
    hint: string;
    message: string;
    status: number;
  };
}

/**
 * Every error this site's own endpoints return is JSON with a stable code, a human message and a
 * hint on what to try next, so an agent can recover without parsing an HTML error page.
 */
export function buildApiError(
  status: number,
  code: string,
  message: string,
  hint: string,
): ApiErrorBody {
  return {
    error: {
      code,
      documentation: `${DOCS_URL}/openapi.json`,
      hint,
      message,
      status,
    },
  };
}

export function jsonError(
  status: number,
  code: string,
  message: string,
  hint: string,
): Response {
  return Response.json(buildApiError(status, code, message, hint), {
    status,
    headers: { 'Cache-Control': 'no-store' },
  });
}

export function apiNotFound(pathname: string): Response {
  return jsonError(
    404,
    'not_found',
    `No endpoint exists at ${pathname}.`,
    `This site exposes /api/search (GET, ?query=) and serves the ByteChef public API specifications at ${DOCS_URL}/openapi.json; every specification is listed in ${DOCS_URL}/.well-known/api-catalog.`,
  );
}
