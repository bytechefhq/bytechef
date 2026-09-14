import { buildApiCatalog } from '@/lib/agent/openapi-specs';

export const revalidate = false;

/** RFC 9727: the catalog of every API description this site publishes. */
export async function GET() {
  return Response.json(await buildApiCatalog(), {
    headers: { 'Content-Type': 'application/linkset+json' },
  });
}
