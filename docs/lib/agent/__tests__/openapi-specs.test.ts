import { describe, expect, it } from 'vitest';
import {
  buildApiCatalog,
  DEFAULT_SPEC,
  isSpecName,
  loadOpenApiDocument,
  SPECS,
  transformOpenApiDocument,
} from '../openapi-specs';

describe('transformOpenApiDocument', () => {
  it('puts the Cloud origin in front of every relative server and keeps the rest', () => {
    const document = transformOpenApiDocument(`openapi: 3.0.1
info:
  title: Test
servers:
  - url: /api/embedded/v1
  - url: https://example.com/other
`);

    expect(document.servers).toEqual([
      { description: 'ByteChef Cloud', url: 'https://app.bytechef.io/api/embedded/v1' },
      {
        description: 'Self-hosted ByteChef, relative to your own instance',
        url: '/api/embedded/v1',
      },
      { url: 'https://example.com/other' },
    ]);
  });

  it('rejects a document that is not OpenAPI', () => {
    expect(() => transformOpenApiDocument('title: nope')).toThrow('not an OpenAPI');
  });
});

describe('published specifications', () => {
  it('reads every spec from the server tree and every one is a real OpenAPI document', async () => {
    for (const name of Object.keys(SPECS)) {
      expect(isSpecName(name)).toBe(true);

      const document = await loadOpenApiDocument(name as keyof typeof SPECS);

      expect(document.openapi).toMatch(/^3\./);
      expect(document.info?.title).toBeTruthy();
      expect(document.servers?.[0]?.url.startsWith('https://app.bytechef.io/')).toBe(true);
    }

    expect(isSpecName('nope')).toBe(false);
    expect(DEFAULT_SPEC).toBe('embedded');
  });

  it('lists every spec in the RFC 9727 catalog', async () => {
    const catalog = (await buildApiCatalog()) as {
      linkset: { anchor: string; 'service-desc': { href: string; title?: string }[] }[];
    };
    const [entry] = catalog.linkset;

    expect(entry.anchor).toBe('https://docs.bytechef.io/');
    expect(entry['service-desc'][0].href).toBe('https://docs.bytechef.io/openapi.json');

    for (const name of Object.keys(SPECS)) {
      expect(entry['service-desc'].map((link) => link.href)).toContain(
        `https://docs.bytechef.io/openapi/${name}.json`,
      );
    }
  });
});
