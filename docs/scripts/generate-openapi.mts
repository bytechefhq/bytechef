import { readFile, readdir, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { generateFiles } from 'fumadocs-openapi';
import { GROUP_META, planGroups } from '../lib/openapi/groups.ts';
import { openapi } from '../lib/openapi/index.ts';

const CONTENT_DIR = path.resolve(import.meta.dirname, '../content/docs');
const OUTPUT_DIR = path.join(CONTENT_DIR, 'openapi');

function collectEntryPaths(entries: { type?: string; path?: string; entries?: unknown[] }[]): string[] {
  const paths: string[] = [];

  for (const entry of entries) {
    if (entry.type === 'group') {
      paths.push(...collectEntryPaths((entry.entries ?? []) as typeof entries));
    } else if (entry.path) {
      paths.push(entry.path.split(path.sep).join('/'));
    }
  }

  return paths;
}

async function main() {
  const { groupIdOf, groups } = await planGroups();

  const expectedGroups = groups.map(({ id }) => id);

  const undocumented = groups.filter(({ metaKey }) => !GROUP_META[metaKey]);

  if (undocumented.length > 0) {
    throw new Error(
      `No GROUP_META entry for: [${undocumented.map(({ metaKey }) => metaKey).join(', ')}]. Add one per group.`,
    );
  }

  // Never `rm -rf` OUTPUT_DIR itself — it holds the hand-written openapi/index.mdx and
  // openapi/meta.json. Everything below it is generated, so clear whole directories rather than the
  // expected groups alone: a renamed or renested group would otherwise leave its old pages behind,
  // out of the nav but still routable.
  const stale = await readdir(OUTPUT_DIR, { withFileTypes: true }).catch(() => []);

  await Promise.all(
    stale
      .filter((entry) => entry.isDirectory())
      .map((entry) => rm(path.join(OUTPUT_DIR, entry.name), { recursive: true, force: true })),
  );

  await generateFiles({
    input: openapi,
    output: OUTPUT_DIR,
    per: 'operation',
    groupBy: groupIdOf,
    // One index page per group, so /openapi/<group> is a real page (openapi/index.mdx links to it)
    // instead of a folder with no landing page. `only` takes leaf file paths rather than the schema
    // id: with `groupBy`, a schema's entries are group nodes, and the card writer skips those.
    index: {
      items: (context) =>
        groups.map(({ id, metaKey, schemaId }) => ({
          path: `${id}/index.mdx`,
          title: GROUP_META[metaKey].title,
          description: GROUP_META[metaKey].description,
          // generatedEntries is keyed by schema, so a spec split across several groups yields every
          // one of its operations here. Each generated path starts with the directory its group was
          // written to, which is what narrows them back down -- look the entries up by group id and
          // they come back undefined, leaving an index page with no operation cards on it.
          only: collectEntryPaths(context.generatedEntries[schemaId] ?? []).filter((entryPath) =>
            entryPath.startsWith(`${id}/`),
          ),
        })),
      // `entry.path` is relative to the output dir, e.g. "automation-project-git/pullProjectFromGit.mdx".
      url: (filePath) => `/openapi/${filePath.replace(/\.mdx$/, '')}`,
    },
  });

  const missing = (
    await Promise.all(
      expectedGroups.map(async (group) =>
        (await readdir(path.join(OUTPUT_DIR, group)).catch(() => null)) ? null : group,
      ),
    )
  ).filter((group) => group !== null);

  if (missing.length > 0) {
    throw new Error(`Expected groups in ${OUTPUT_DIR}, missing: [${missing.join(', ')}].`);
  }

  await Promise.all(
    groups.map(({ id, metaKey }) =>
      writeFile(
        path.join(OUTPUT_DIR, id, 'meta.json'),
        JSON.stringify(
          (({ title, navTitle, description }) => ({
            title: navTitle ?? title,
            description,
            pages: ['...'],
          }))(GROUP_META[metaKey]),
          null,
          2,
        ) + '\n',
      ),
    ),
  );

  const FOLDER_META: Record<string, { title: string; description: string }> = {
    backend: {
      title: 'Backend (API Key)',
      description: "Called from your own server, naming the user in the path.",
    },
    frontend: {
      title: 'Frontend (Signing Key JWT)',
      description: 'Called from the browser, naming the user through the token.',
    },
  };

  // `pages: ['...']` would sort a folder's children by directory order. Order them by where their
  // entry sits in GROUP_META instead, so the declaration order above is the sidebar order and the
  // two folders list the same resources in the same sequence.
  const metaKeyOrder = Object.keys(GROUP_META);

  await Promise.all(
    Object.entries(FOLDER_META).map(([folder, meta]) =>
      writeFile(
        path.join(OUTPUT_DIR, folder, 'meta.json'),
        JSON.stringify(
          {
            ...meta,
            pages: groups
              .filter(({ id }) => id.startsWith(`${folder}/`))
              .sort((a, b) => metaKeyOrder.indexOf(a.metaKey) - metaKeyOrder.indexOf(b.metaKey))
              .map(({ id }) => id.slice(folder.length + 1)),
          },
          null,
          2,
        ) + '\n',
      ),
    ),
  );

  // fumadocs-openapi's index writer emits only title and description, so a group whose feature is
  // not in the latest release gets its comingSoon frontmatter added here. The docs page component
  // reads that flag to render the badge and the warning callout.
  const comingSoonPages = groups.flatMap(({ id, metaKey }) => {
    const { comingSoon, comingSoonOperations = [] } = GROUP_META[metaKey];

    return [
      ...(comingSoon ? [path.join(OUTPUT_DIR, id, 'index.mdx')] : []),
      ...comingSoonOperations.map((operationId) => path.join(OUTPUT_DIR, id, `${operationId}.mdx`)),
    ];
  });

  await Promise.all(
    comingSoonPages.map(async (pagePath) => {
      const content = await readFile(pagePath, 'utf8').catch(() => {
        // A renamed or removed operationId would otherwise be flagged silently by doing nothing.
        throw new Error(`Cannot mark ${pagePath} as coming soon: no such generated page.`);
      });

      if (!content.startsWith('---\n')) {
        throw new Error(`Expected frontmatter at the top of ${pagePath}, got: ${content.slice(0, 40)}`);
      }

      await writeFile(pagePath, `---\ncomingSoon: true\n${content.slice(4)}`);
    }),
  );

  console.log(`OpenAPI pages generated under ${OUTPUT_DIR} (groups: ${expectedGroups.join(', ')})`);
}

// Runs under plain `node --experimental-strip-types` (and bun). Deliberately NOT tsx:
// tsx's loader hooks mis-interop fumadocs-openapi's CJS dependency dereference-json-schema
// (__esModule:true with no exports.default -> default import lands undefined), which
// surfaces as "Cannot read properties of undefined (reading 'resolveRefSync')".
main().catch((error) => {
  console.error(error);
  process.exit(1);
});
