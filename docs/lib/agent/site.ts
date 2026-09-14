/**
 * Facts about this site that the agent-facing responses (404 bodies, API errors, the API catalog)
 * share. Kept free of framework imports so the tests can load it in plain Node.
 */
export const DOCS_URL = 'https://docs.bytechef.io';

export function absoluteUrl(pathname: string): string {
  if (pathname.startsWith('http://') || pathname.startsWith('https://')) {
    return pathname;
  }

  return `${DOCS_URL}${pathname === '/' ? '' : pathname}`;
}

/** Where a person or an agent should look after landing on a page that does not exist. */
export const RECOVERY_LINKS = [
  { href: '/platform', label: 'Quick Start, the documentation index' },
  { href: '/reference', label: 'Reference for every component and the expression syntax' },
  { href: '/llms.txt', label: 'llms.txt, every page with its description' },
  { href: '/llms-full.txt', label: 'llms-full.txt, the whole documentation as one markdown file' },
  { href: '/sitemap.xml', label: 'Sitemap' },
  { href: '/openapi.json', label: 'OpenAPI specification of the public API' },
  { href: 'https://www.bytechef.io', label: 'ByteChef product site' },
] as const;

/** Collapses duplicate and trailing slashes so the path reads cleanly in a 404 body. */
export function normalizePathname(pathname: string): string {
  const collapsed = `/${pathname}`.replace(/\/{2,}/g, '/');

  return collapsed.length > 1 ? collapsed.replace(/\/+$/, '') : collapsed;
}

export function buildNotFoundMarkdown(pathname: string): string {
  const links = RECOVERY_LINKS.map(
    ({ href, label }) => `- [${label}](${absoluteUrl(href)})`,
  ).join('\n');

  return `# 404: page not found

There is no page at \`${normalizePathname(pathname)}\` on ${DOCS_URL}.

Where to look next:

${links}
`;
}
