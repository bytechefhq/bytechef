/**
 * Keeps `.agents/coming-soon-inventory.md` honest.
 *
 *   node --experimental-strip-types ./scripts/coming-soon.mts          # check, exits 1 on drift
 *   node --experimental-strip-types ./scripts/coming-soon.mts --write  # regenerate the table
 *
 * The whole-page table is derived from `comingSoon: true` frontmatter, and the Sidebar column from
 * walking every parent `meta.json` — a page is only reachable when each folder above it is listed.
 *
 * Commented-out sections are found by the `@coming-soon` marker, NOT by prose. The registry used to
 * match on the words "coming soon" appearing inside a `{/* … *\/}` block, which meant rewording a
 * comment silently dropped its entry — that is how the observability section went missing once.
 * A marker cannot be reworded away, and `checkMarkers` fails the run when a block that is
 * structurally a hidden section (it contains a heading or a table row) lacks one.
 */
import { readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';

const DOCS_ROOT = join(import.meta.dirname, '..', 'content', 'docs');
const INVENTORY = join(import.meta.dirname, '..', '..', '.agents', 'coming-soon-inventory.md');
const MARKER = '@coming-soon';

interface Page {
  url: string;
  title: string;
  hidden: boolean;
}

function walk(dir: string, out: string[] = []): string[] {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);

    if (statSync(full).isDirectory()) {
      walk(full, out);
    } else if (entry.endsWith('.mdx')) {
      out.push(full);
    }
  }

  return out;
}

function frontmatter(source: string): string {
  return /^---\n([\s\S]*?)\n---/.exec(source)?.[1] ?? '';
}

function readPages(dir: string): string[] | undefined {
  try {
    return JSON.parse(readFileSync(join(dir, 'meta.json'), 'utf8')).pages;
  } catch {
    return undefined;
  }
}

/**
 * A page is hidden when some `meta.json` above it fails to name a route that reaches it.
 *
 * An entry may be a sibling name, a folder name, or a nested path that pulls a deeper page up into
 * this level's ordering — `automation/meta.json` lists both `build/workflows` and `build/copilot`
 * while never listing bare `build`. So the test at each level is whether ANY prefix of the page's
 * relative path appears, not whether the immediate child folder does; checking only the first
 * segment marked every page under `build/` as hidden.
 */
function isHidden(file: string): boolean {
  const withoutExtension = file.slice(0, -'.mdx'.length);
  let dir = file.slice(0, file.lastIndexOf('/'));

  for (;;) {
    const pages = readPages(dir);

    if (pages && !pages.includes('...')) {
      const segments = relative(dir, withoutExtension).split('/');
      const reachable = segments.some((_, index) => {
        const prefix = segments.slice(0, index + 1).join('/');

        // An index page is addressed either by its folder or by the explicit `<folder>/index`.
        return pages.includes(prefix) || pages.includes(prefix.replace(/\/index$/, ''));
      });

      if (!reachable) {
        return true;
      }
    }

    if (dir === DOCS_ROOT) {
      return false;
    }

    dir = dir.slice(0, dir.lastIndexOf('/'));
  }
}

function toUrl(file: string): string {
  return '/' + relative(DOCS_ROOT, file).replace(/\.mdx$/, '').replace(/\/index$/, '');
}

function collect(): { pages: Page[]; unmarked: string[] } {
  const pages: Page[] = [];
  const unmarked: string[] = [];

  for (const file of walk(DOCS_ROOT)) {
    const source = readFileSync(file, 'utf8');

    if (frontmatter(source).includes('comingSoon: true')) {
      pages.push({
        url: toUrl(file),
        title: /^title:\s*"?(.*?)"?\s*$/m.exec(frontmatter(source))?.[1] ?? '',
        hidden: isHidden(file),
      });
    }

    for (const block of source.match(/\{\/\*[\s\S]*?\*\/\}/g) ?? []) {
      const structural = /^#{2,4} /m.test(block) || /^\|/m.test(block);

      if (structural && !block.includes(MARKER)) {
        unmarked.push(`${toUrl(file)} — commented-out section without a ${MARKER} marker`);
      }
    }
  }

  return { pages: pages.sort((a, b) => a.url.localeCompare(b.url)), unmarked };
}

function bucket(url: string): string {
  if (url.startsWith('/openapi')) return 'API Reference';
  if (url.startsWith('/developer-guide')) return 'Developer Guide';
  if (url.startsWith('/platform/settings')) return 'Platform Settings';
  if (url.startsWith('/platform/your-account')) return 'Your Account';
  if (url.startsWith('/platform/use-bytechef')) return 'Self-Hosted';
  if (url.startsWith('/platform/embedded')) return 'Embedded';

  return 'Automation';
}

const ORDER = [
  'Automation',
  'Embedded',
  'Platform Settings',
  'Self-Hosted',
  'Your Account',
  'API Reference',
  'Developer Guide',
];

function render(pages: Page[], inventory: string): string {
  // The Edition column is editorial; carry forward whatever is already recorded per URL.
  const editions = new Map<string, string>();

  for (const [, url, edition] of inventory.matchAll(/\]\((\/[^)]+)\)\s*\|\s*(CE|EE)\s*\|/g)) {
    editions.set(url, edition);
  }

  const lines = ['## Whole-page Coming Soon', ''];

  for (const group of ORDER) {
    const rows = pages.filter((page) => bucket(page.url) === group);

    if (rows.length === 0) {
      continue;
    }

    lines.push(`### ${group}`, '', '| Page | Path | Edition | Sidebar |', '|---|---|---|---|');

    for (const { url, title, hidden } of rows) {
      lines.push(
        `| ${title} | [\`${url}\`](${url}) | ${editions.get(url) ?? '—'} | ${hidden ? '**hidden**' : 'visible'} |`,
      );
    }

    lines.push('');
  }

  return lines.join('\n').trimEnd() + '\n';
}

const { pages, unmarked } = collect();
const inventory = readFileSync(INVENTORY, 'utf8');
const hidden = pages.filter((page) => page.hidden).length;

const rebuilt =
  inventory.slice(0, inventory.indexOf('## Whole-page Coming Soon')) +
  render(pages, inventory) +
  '\n---\n\n' +
  inventory.slice(inventory.indexOf('## Partial'));

const counted = rebuilt.replace(
  /\*\*\d+ whole-page\*\* \(\d+ hidden\)/,
  `**${pages.length} whole-page** (${hidden} hidden)`,
);

if (process.argv.includes('--write')) {
  writeFileSync(INVENTORY, counted);
  console.log(`coming-soon: wrote ${pages.length} pages (${hidden} hidden)`);
} else {
  const drifted = counted !== inventory;

  for (const problem of unmarked) {
    console.error(`coming-soon: ${problem}`);
  }

  if (drifted) {
    console.error('coming-soon: inventory is out of date — run `npm run coming-soon:write`');
  }

  if (drifted || unmarked.length > 0) {
    process.exit(1);
  }

  console.log(`coming-soon: up to date — ${pages.length} pages (${hidden} hidden)`);
}
