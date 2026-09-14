/**
 * Every page is served as HTML or markdown from the same URL, negotiated on Accept by
 * `fumadocs-core/negotiation` in proxy.ts. A response that took part in that negotiation must say
 * so with `Vary: Accept`, or a CDN could hand the cached HTML to an agent, or the cached markdown
 * to a browser.
 */
export const MARKDOWN_MEDIA_TYPE = 'text/markdown';

export const NEGOTIATED_VARY = 'Accept, Accept-Encoding';

/** Merges `value` into an existing Vary header without duplicating entries. */
export function mergeVary(
  existing: string | null | undefined,
  value: string,
): string {
  const entries = new Set<string>();

  for (const sourceValue of [existing ?? '', value]) {
    for (const entry of sourceValue.split(',')) {
      const trimmed = entry.trim();

      if (trimmed) entries.add(trimmed);
    }
  }

  return [...entries].join(', ');
}

/** Sets Vary on a Headers object, keeping whatever the framework already put there. */
export function applyNegotiatedVary(headers: Headers): void {
  headers.set('Vary', mergeVary(headers.get('Vary'), NEGOTIATED_VARY));
}

export function markdownResponse(body: string, status = 200): Response {
  return new Response(body, {
    status,
    headers: {
      'Content-Type': `${MARKDOWN_MEDIA_TYPE}; charset=utf-8`,
      Vary: NEGOTIATED_VARY,
    },
  });
}
