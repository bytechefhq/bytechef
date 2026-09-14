import { describe, expect, it } from 'vitest';
import { apiNotFound, buildApiError, jsonError } from '../api-error';

describe('api errors', () => {
  it('builds a body with code, message, hint, status and a documentation link', () => {
    expect(buildApiError(400, 'bad_request', 'Missing query.', 'Pass ?query=.')).toEqual({
      error: {
        code: 'bad_request',
        documentation: 'https://docs.bytechef.io/openapi.json',
        hint: 'Pass ?query=.',
        message: 'Missing query.',
        status: 400,
      },
    });
  });

  it('serves JSON with the status and no caching', async () => {
    const response = jsonError(418, 'teapot', 'No.', 'Try coffee.');

    expect(response.status).toBe(418);
    expect(response.headers.get('Content-Type')).toContain('application/json');
    expect(response.headers.get('Cache-Control')).toBe('no-store');
    expect((await response.json()).error.code).toBe('teapot');
  });

  it('answers unknown API paths with a 404 that points at the catalog', async () => {
    const response = apiNotFound('/api/nope');
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('not_found');
    expect(body.error.message).toContain('/api/nope');
    expect(body.error.hint).toContain('/.well-known/api-catalog');
  });
});
