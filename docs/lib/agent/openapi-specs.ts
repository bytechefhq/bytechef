import { readFile } from 'node:fs/promises';
import { parse as parseYaml } from 'yaml';
import { DEFAULT_SPEC, isSpecName, SPECS, type SpecName } from '@/lib/openapi/specs';
import { DOCS_URL } from './site';

export { DEFAULT_SPEC, isSpecName, SPECS, type SpecName };

export const CLOUD_ORIGIN = 'https://app.bytechef.io';

interface Server {
  description?: string;
  url: string;
}

export interface OpenApiDocument {
  openapi?: string;
  info?: { title?: string; version?: string };
  servers?: Server[];
  [key: string]: unknown;
}

/**
 * Turns the repository's YAML into the JSON document served here. The specs declare a relative
 * server such as `/api/embedded/v1`, right for a self-hosted instance but useless to an agent
 * reading the spec from this site, so the ByteChef Cloud origin is added in front of it.
 */
export function transformOpenApiDocument(yamlText: string): OpenApiDocument {
  const document = parseYaml(yamlText) as OpenApiDocument;

  if (
    !document ||
    typeof document !== 'object' ||
    typeof document.openapi !== 'string'
  ) {
    throw new Error('The document is not an OpenAPI specification.');
  }

  const servers = document.servers ?? [];
  const relativeServers = servers.filter((server) => server.url.startsWith('/'));

  return {
    ...document,
    servers: [
      ...relativeServers.map((server) => ({
        description: 'ByteChef Cloud',
        url: `${CLOUD_ORIGIN}${server.url}`,
      })),
      ...relativeServers.map((server) => ({
        description: 'Self-hosted ByteChef, relative to your own instance',
        url: server.url,
      })),
      ...servers.filter((server) => !server.url.startsWith('/')),
    ],
  };
}

export async function loadOpenApiDocument(name: SpecName): Promise<OpenApiDocument> {
  return transformOpenApiDocument(await readFile(SPECS[name], 'utf8'));
}

export function specUrl(name: SpecName): string {
  return `${DOCS_URL}/openapi/${name}.json`;
}

export function openApiResponse(document: OpenApiDocument): Response {
  return Response.json(document, {
    headers: {
      'Cache-Control': 'public, max-age=3600',
      'Content-Type': 'application/openapi+json; charset=utf-8',
    },
  });
}

/** RFC 9727 API catalog: a linkset (RFC 9264) naming every published API description. */
export async function buildApiCatalog(): Promise<Record<string, unknown>> {
  const entries = await Promise.all(
    (Object.keys(SPECS) as SpecName[]).map(async (name) => {
      const document = await loadOpenApiDocument(name);

      return {
        href: specUrl(name),
        title: document.info?.title ?? name,
        type: 'application/openapi+json',
      };
    }),
  );

  return {
    linkset: [
      {
        anchor: `${DOCS_URL}/`,
        'service-desc': [
          { href: `${DOCS_URL}/openapi.json`, type: 'application/openapi+json' },
          ...entries,
        ],
        'service-doc': [{ href: `${DOCS_URL}/platform`, type: 'text/html' }],
        'service-meta': [{ href: `${DOCS_URL}/llms.txt`, type: 'text/plain' }],
      },
    ],
  };
}
