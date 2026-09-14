import { jsonError } from '@/lib/agent/api-error';
import {
  isSpecName,
  loadOpenApiDocument,
  openApiResponse,
  SPECS,
} from '@/lib/agent/openapi-specs';

export const revalidate = false;

export function generateStaticParams() {
  return Object.keys(SPECS).map((name) => ({ spec: `${name}.json` }));
}

/** `/openapi/<name>.json` for every published specification. */
export async function GET(
  _request: Request,
  { params }: { params: Promise<{ spec: string }> },
) {
  const { spec } = await params;
  const name = spec.replace(/\.json$/, '');

  if (!isSpecName(name)) {
    return jsonError(
      404,
      'unknown_specification',
      `No OpenAPI specification named ${name}.`,
      `Available specifications: ${Object.keys(SPECS)
        .map((key) => `/openapi/${key}.json`)
        .join(', ')}.`,
    );
  }

  return openApiResponse(await loadOpenApiDocument(name));
}
