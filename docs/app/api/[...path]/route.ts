import { apiNotFound } from '@/lib/agent/api-error';

/** Any API path that no route handler claims answers with a JSON 404, never the HTML app shell. */
function handle(request: Request) {
  return apiNotFound(new URL(request.url).pathname);
}

export const GET = handle;
export const POST = handle;
export const PUT = handle;
export const PATCH = handle;
export const DELETE = handle;
export const OPTIONS = handle;
export const HEAD = handle;
