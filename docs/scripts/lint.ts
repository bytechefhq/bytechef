import {
  type FileObject,
  printErrors,
  scanURLs,
  validateFiles,
} from 'next-validate-link';
import { InferPageType } from 'fumadocs-core/source';
import { source } from '@/lib/source';

type AnySource = typeof source;

async function checkLinks() {
  const scanned = await scanURLs({
    populate: {
      // Keyed by the route's directory path, route group included: scanURLs matches by popping
      // segments off the right, so a bare '[...slug]' never matches '(docs)/[...slug]'.
      '(docs)/[...slug]': await Promise.all(
        source.getPages().map(async (page) => {
          return {
            value: {
              slug: page.slugs,
            },
            hashes: await getHeadings(page),
          };
        }),
      ),
    },
  });

  console.log(
    `collected ${scanned.urls.size} URLs, ${scanned.fallbackUrls.length} fallbacks`,
  );

  // `pathToUrl` returning undefined means "this path maps to no page". next-validate-link
  // treats that as *undeterminable* rather than *broken*, so an unresolvable relative link is
  // silently dropped instead of reported — a `[x](totally-not-a-page)` canary passes a run that
  // prints "0 errors". Absolute links are unaffected. Check relative targets ourselves.
  const relativeErrors = await checkRelativeLinks(source);

  if (relativeErrors.length > 0) {
    console.error(`\n${relativeErrors.length} unresolved relative link(s):`);

    for (const { file, target } of relativeErrors) {
      console.error(`  ${file}: ${target}`);
    }
  }

  printErrors(
    await validateFiles(
      [...(await getFiles(source))],
      {
        scanned,
        markdown: {
          components: {
            Card: { attributes: ['href'] },
          },
        },
        checkRelativePaths: 'as-url',
        // Mirror how the site itself resolves links: anything not rooted at "/" is resolved
        // against the *file's* directory (see `source.getPageByHref(href, { dir })` in
        // app/(docs)/[...slug]/page.tsx), including bare hrefs like `skills`. The library's
        // default treats those as root-absolute URLs, and resolves `./x` against the parent of
        // the page URL — which is wrong for a folder's index page.
        determinatePathname: (pathname) =>
          pathname.startsWith('/') ? 'url' : 'relative-file-path',
        pathToUrl: createPathToUrl(source),
      },
    ),
    true,
  );

  if (relativeErrors.length > 0) process.exit(1);
}

/**
 * Resolves every relative markdown link against its own file's directory and reports the ones
 * that hit no page and no asset. Deliberately separate from next-validate-link: see the note at
 * the call site for why that library cannot report these.
 */
async function checkRelativeLinks(
  source: AnySource,
): Promise<{ file: string; target: string }[]> {
  const { existsSync } = await import('node:fs');
  const { dirname, resolve } = await import('node:path');

  // `resolve()` always returns an absolute path, so `known` must hold absolute paths too —
  // `info.fullPath` is repo-relative, and comparing the two forms silently matches nothing,
  // which turns every relative link into a false positive.
  const known = new Set<string>();

  for (const page of source.getPages()) {
    const withoutExtension = resolve(page.data.info.fullPath).replace(/\.mdx?$/, '');

    known.add(withoutExtension);

    if (withoutExtension.endsWith('/index')) {
      known.add(withoutExtension.slice(0, -'/index'.length));
    }
  }

  const errors: { file: string; target: string }[] = [];

  for (const page of source.getPages()) {
    const filePath = page.data.info.fullPath;
    const content = (await page.data.getText('raw')).replace(/```[\s\S]*?```/g, '');

    for (const match of content.matchAll(/\[[^\]]*\]\(([^)\s]+)\)/g)) {
      const target = match[1];

      if (/^(https?:|\/|#|mailto:|data:)/.test(target)) continue;

      const withoutHash = target.split('#')[0].split('?')[0];

      if (!withoutHash) continue;

      const candidate = resolve(dirname(filePath), withoutHash);

      if (
        known.has(candidate) ||
        known.has(candidate.replace(/\.mdx?$/, '')) ||
        existsSync(candidate)
      ) {
        continue;
      }

      errors.push({ file: filePath, target });
    }
  }

  return errors;
}

/**
 * Maps a content file path back to its page URL, tolerating the extension-less and
 * directory (`index.md`) forms a relative link can take.
 */
function createPathToUrl(source: AnySource): (filePath: string) => string | undefined {
  const urlByPath = new Map<string, string>();

  for (const page of source.getPages()) {
    const fullPath = page.data.info.fullPath;
    const withoutExtension = fullPath.replace(/\.mdx?$/, '');

    urlByPath.set(fullPath, page.url);
    urlByPath.set(withoutExtension, page.url);

    if (withoutExtension.endsWith('/index')) {
      urlByPath.set(withoutExtension.slice(0, -'/index'.length), page.url);
    }
  }

  return (filePath) =>
    urlByPath.get(filePath) ?? urlByPath.get(filePath.replace(/\.mdx?$/, ''));
}

async function getHeadings({
  data,
}: InferPageType<AnySource>): Promise<string[]> {
  const { _exports, toc } = await data.load();
  const headings = toc.map((item) => item.url.slice(1));
  const elementIds = _exports?.elementIds;
  if (Array.isArray(elementIds)) {
    headings.push(...elementIds);
  }

  return headings;
}

async function getFiles(source: AnySource) {
  const files: FileObject[] = [];
  for (const page of source.getPages()) {
    files.push({
      data: page.data,
      url: page.url,
      path: page.data.info.fullPath,
      content: await page.data.getText('raw'),
    });
  }

  return files;
}

void checkLinks();
