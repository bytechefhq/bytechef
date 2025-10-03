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
      '[...slug]': source.getPages().map((page) => {
        return {
          value: {
            slug: page.slugs,
          },
          hashes: getHeadings(page),
        };
      }),
    },
  });

  console.log(
    `collected ${scanned.urls.size} URLs, ${scanned.fallbackUrls.length} fallbacks`,
  );

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
      },
    ),
    true,
  );
}

function getHeadings({ data }: InferPageType<AnySource>) {
  const headings = data.toc.map((item) => item.url.slice(1));
  const elementIds = data._exports?.elementIds;
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
      path: page.absolutePath,
      content: await page.data.getText('raw'),
    });
  }

  return files;
}

void checkLinks();
