import type { GeneratedPageProps } from 'fumadocs-openapi';
import { openapi } from '@/lib/openapi';
import { APIPage } from '@/components/api-page';

/**
 * Server-side bridge for the MDX pages emitted by `generateFiles()`, which reference the operation
 * by schema id (`document="automation"`). Since fumadocs-openapi v11 the page renderer is a client
 * component that takes an already-bundled document, so resolving the id is the app's job.
 */
export async function OpenAPIPage({ document, ...props }: GeneratedPageProps) {
  const { bundled } = await openapi.getSchema(document);

  return (
    <APIPage
      {...props}
      payload={{ bundled, proxyUrl: openapi.options.proxyUrl }}
    />
  );
}
