import { DEFAULT_SPEC, loadOpenApiDocument, openApiResponse } from '@/lib/agent/openapi-specs';

export const revalidate = false;

/** The default public API specification; every other one is at /openapi/<name>.json. */
export async function GET() {
  return openApiResponse(await loadOpenApiDocument(DEFAULT_SPEC));
}
