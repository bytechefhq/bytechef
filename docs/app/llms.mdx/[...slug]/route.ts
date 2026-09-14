import { type NextRequest } from 'next/server';
import { getLLMText } from '@/lib/get-llm-text';
import { source } from '@/lib/source';
import { markdownResponse } from '@/lib/agent/content-negotiation';
import { buildNotFoundMarkdown } from '@/lib/agent/site';

export const revalidate = false;

export async function GET(
  _req: NextRequest,
  { params }: RouteContext<'/llms.mdx/[...slug]'>,
) {
  const slug = (await params).slug;
  const page = source.getPage(slug);
  // Still a real 404, but with a markdown body an agent can act on instead of an empty one.
  if (!page) return markdownResponse(buildNotFoundMarkdown(`/${slug.join('/')}`), 404);

  // Vary: Accept because proxy.ts picked this variant from the Accept header of a page URL.
  return markdownResponse(await getLLMText(page));
}

export function generateStaticParams() {
  return source.generateParams();
}
