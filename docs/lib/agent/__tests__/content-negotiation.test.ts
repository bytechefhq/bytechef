import { describe, expect, it } from 'vitest';
import {
  applyNegotiatedVary,
  markdownResponse,
  mergeVary,
  NEGOTIATED_VARY,
} from '../content-negotiation';

describe('mergeVary', () => {
  it('adds Accept and Accept-Encoding without duplicating what is already there', () => {
    expect(mergeVary(null, NEGOTIATED_VARY)).toBe('Accept, Accept-Encoding');
    expect(mergeVary('RSC, Accept-Encoding', NEGOTIATED_VARY)).toBe(
      'RSC, Accept-Encoding, Accept',
    );
    expect(mergeVary('Accept', NEGOTIATED_VARY)).toBe('Accept, Accept-Encoding');
  });

  it('applies to a Headers object in place', () => {
    const headers = new Headers({ Vary: 'RSC' });

    applyNegotiatedVary(headers);

    expect(headers.get('Vary')).toBe('RSC, Accept, Accept-Encoding');
  });
});

describe('markdownResponse', () => {
  it('serves text/markdown with Vary: Accept and the requested status', async () => {
    const response = markdownResponse('# Hi', 404);

    expect(response.status).toBe(404);
    expect(response.headers.get('Content-Type')).toBe('text/markdown; charset=utf-8');
    expect(response.headers.get('Vary')).toBe('Accept, Accept-Encoding');
    expect(await response.text()).toBe('# Hi');
  });
});
