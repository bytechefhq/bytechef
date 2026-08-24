import { readFile, readdir, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { generateFiles } from 'fumadocs-openapi';
import { parse as parseYaml } from 'yaml';
import { openapi, SPECS } from '../lib/openapi/index.ts';

const CONTENT_DIR = path.resolve(import.meta.dirname, '../content/docs');
const OUTPUT_DIR = path.join(CONTENT_DIR, 'openapi');
// Every spec is grouped by the tags it declares, so the sidebar mirrors the grouping the specs
// themselves express rather than one this file invents. Group ids are `<schemaId>-<tag>`, except
// that a spec declaring a single tag stays one group under its own id -- `custom-components` rather
// than `custom-components-custom-component`. Retagging an operation moves it between groups without
// touching this file; adding a tag needs a GROUP_META entry, and the build says so if you forget.
//
// Groups split again by audience, so the two ways into the same resource are documented apart
// rather than interleaved. An operation reachable only with a Signing Key JWT is browser-facing and
// lands in `<group>-frontend`; one that accepts an API Key is callable from your backend and keeps
// the bare id. The specs declare this: a JWT resolves the user from the token's `sub` claim, so
// those paths carry no `{externalUserId}` segment, while an API Key needs that segment to know whom
// it is acting for.

const GROUP_META: Record<
  string,
  {
    title: string;
    navTitle?: string;
    description: string;
    comingSoon?: boolean;
    comingSoonOperations?: string[];
  }
> = {
  'embedded-action': {
    title: 'Embedded Actions',
    navTitle: 'Actions',
    description: 'Execute a component action for a connected user and return its result.',
  },
  'embedded-tool': {
    title: 'Embedded Tools',
    navTitle: 'Tools',
    description: "List a connected user's available tools, and execute one.",
  },
  'embedded-configuration-integration': {
    title: 'Embedded Integrations',
    navTitle: 'Integrations',
    description: "List and fetch the integrations available to your customers.",
  },
  'embedded-configuration-integration-instance': {
    title: 'Embedded Integration Instances',
    navTitle: 'Integration Instances',
    description: "Create and delete a connected user's instance of an integration.",
  },
  'embedded-configuration-integration-instance-workflow': {
    title: 'Embedded Integration Instance Workflows',
    navTitle: 'Instance Workflows',
    description: 'Enable, disable and configure the workflows of an integration instance.',
  },
  'embedded-configuration-connection': {
    title: 'Embedded Connections',
    navTitle: 'Connections',
    description: "List, delete and reauthorize a connected user's connections.",
  },
  'embedded-configuration-connected-user': {
    title: 'Embedded Connected Users',
    navTitle: 'Connected Users',
    description: 'Update a connected user.',
  },
  'embedded-configuration-connected-user-project-workflow': {
    title: 'Embedded Connected User Workflows',
    navTitle: 'User Workflows',
    description: "Create, update and manage a connected user's own workflows, including generation from a prompt.",
  },
  'embedded-configuration-automation-workflow-project': {
    title: 'Embedded Workflow Catalog',
    navTitle: 'Workflow Catalog',
    description: 'List the catalog projects available to connected users.',
  },
  // One entry per tag in the automation spec -- see SPLIT_BY_TAG. The keys are `automation-<tag>`.
  'automation-workflow-execution': {
    title: 'Automation Workflow Executions',
    navTitle: 'Workflow Executions',
    description: 'Fetch workflow executions of a workspace, and a single execution with its full detail.',
    // The executions endpoints are called out as upcoming on
    // /platform/automation/monitor/workflow-executions.
    comingSoonOperations: ['getWorkflowExecution', 'getWorkflowExecutionsPage'],
  },
  'automation-project-code-workflow': {
    title: 'Automation Projects',
    navTitle: 'Projects',
    description: 'Deploy a code-based project.',
    // Deploys a code-based project, whose feature page is itself comingSoon.
    comingSoonOperations: ['deployProject'],
  },
  'automation-project-git': {
    title: 'Automation Git',
    navTitle: 'Git',
    description: "Pull a project from its configured git repository.",
    // Deliberately not comingSoon: settings/git-configuration is not flagged.
  },
  // Browser-facing twins: same resources, reached with a Signing Key JWT.
  'embedded-configuration-integration-frontend': {
    title: 'Embedded Integrations (Frontend)',
    navTitle: 'Integrations',
    description: "List and fetch the integrations available to the signed-in connected user.",
  },
  'embedded-configuration-integration-instance-frontend': {
    title: 'Embedded Integration Instances (Frontend)',
    navTitle: 'Integration Instances',
    description: "Create and delete the signed-in connected user's instance of an integration.",
  },
  'embedded-configuration-integration-instance-workflow-frontend': {
    title: 'Embedded Integration Instance Workflows (Frontend)',
    navTitle: 'Instance Workflows',
    description: "Enable, disable and configure the workflows of the signed-in user's integration instance.",
  },
  'embedded-configuration-connection-frontend': {
    title: 'Embedded Connections (Frontend)',
    navTitle: 'Connections',
    description: "List the signed-in connected user's connections.",
  },
  'embedded-configuration-connected-user-frontend': {
    title: 'Embedded Connected Users (Frontend)',
    navTitle: 'Connected Users',
    description: "Update the signed-in connected user.",
  },
  'embedded-configuration-connected-user-project-workflow-frontend': {
    title: 'Embedded Connected User Workflows (Frontend)',
    navTitle: 'User Workflows',
    description: "Create, update and manage the signed-in user's own workflows, including generation from a prompt.",
  },
  'embedded-configuration-automation-workflow-project-frontend': {
    title: 'Embedded Workflow Catalog (Frontend)',
    navTitle: 'Workflow Catalog',
    description: "List the catalog projects available to the signed-in connected user.",
  },
  'embedded-webhook-app-event-trigger-frontend': {
    title: 'Embedded App Events',
    navTitle: 'App Events',
    description: "Fire an App Event to start every one of a connected user's subscribed workflows.",
  },
  'embedded-webhook-request-trigger-frontend': {
    title: 'Embedded Request Trigger',
    navTitle: 'Request Trigger',
    description: "Execute a single workflow through its Request trigger and return its result.",
  },

  'custom-components': {
    title: 'Custom Components',
    description: 'Public REST API for deploying a custom component to the platform.',
    comingSoon: true,
  },
};

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

interface Operation {
  tag: string;
  frontend: boolean;
}

/**
 * Reads the tag and audience of every operation in a spec, keyed `<METHOD> <path>` -- the only
 * identity fumadocs-openapi hands `groupBy`, which receives
 * `{ schemaId, info, item: { path, method } }` and neither tags nor security.
 */
async function readOperations(schemaId: keyof typeof SPECS) {
  const document = parseYaml(await readFile(SPECS[schemaId], 'utf8'));
  const operations = new Map<string, Operation>();

  for (const [operationPath, pathItem] of Object.entries(document.paths ?? {})) {
    for (const [method, operation] of Object.entries(
      pathItem as Record<string, { tags?: string[]; security?: Record<string, unknown>[] }>,
    )) {
      const tag = operation?.tags?.[0];

      if (!tag) {
        throw new Error(
          `Grouping is by tag, but ${schemaId}'s ${method.toUpperCase()} ${operationPath} declares none.`,
        );
      }

      // An operation that accepts an API Key is backend-callable. One left undeclared is treated as
      // backend too: the automation and platform specs carry no security block, and both take an
      // API Key.
      const schemes = (operation.security ?? []).map((requirement) => Object.keys(requirement)[0]);
      const frontend = schemes.length > 0 && !schemes.includes('bearerAuth');

      operations.set(`${method.toUpperCase()} ${operationPath}`, { tag, frontend });
    }
  }

  return operations;
}

async function main() {
  const operationsBySchema = new Map<string, Map<string, Operation>>();
  // A single-tag spec is already one group; adding its tag would only repeat its own id.
  const taggedSchemas = new Set<string>();

  for (const schemaId of Object.keys(SPECS) as (keyof typeof SPECS)[]) {
    const operations = await readOperations(schemaId);

    operationsBySchema.set(schemaId, operations);

    if (new Set([...operations.values()].map(({ tag }) => tag)).size > 1) {
      taggedSchemas.add(schemaId);
    }
  }

  const idOf = (schemaId: string, { tag, frontend }: Operation) =>
    [schemaId, taggedSchemas.has(schemaId) ? tag : null, frontend ? 'frontend' : null]
      .filter(Boolean)
      .join('-');

  const groupIdOf = (entry: { schemaId: string; item?: { path?: string; method?: string } }) => {
    const key = `${(entry.item?.method ?? '').toUpperCase()} ${entry.item?.path ?? ''}`;
    const operation = operationsBySchema.get(entry.schemaId)?.get(key);

    if (!operation) {
      throw new Error(`No operation found for ${entry.schemaId} ${key}; cannot assign it to a group.`);
    }

    return idOf(entry.schemaId, operation);
  };

  // Each group remembers the spec it came from, because `context.generatedEntries` below is keyed by
  // schema id rather than by the group ids groupBy produces.
  const groups = Object.keys(SPECS).flatMap((schemaId) => {
    const operations = operationsBySchema.get(schemaId) ?? new Map<string, Operation>();
    const ids = [...new Set([...operations.values()].map((operation) => idOf(schemaId, operation)))];

    return ids.map((id) => ({ id, schemaId }));
  });

  const expectedGroups = groups.map(({ id }) => id);

  const undocumented = expectedGroups.filter((group) => !GROUP_META[group]);

  if (undocumented.length > 0) {
    throw new Error(`No GROUP_META entry for: [${undocumented.join(', ')}]. Add one per group.`);
  }

  // Never `rm -rf` OUTPUT_DIR itself — it holds the hand-written openapi/index.mdx and
  // openapi/meta.json.
  await Promise.all(
    expectedGroups.map((group) =>
      rm(path.join(OUTPUT_DIR, group), { recursive: true, force: true }),
    ),
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
        groups.map(({ id, schemaId }) => ({
          path: `${id}/index.mdx`,
          title: GROUP_META[id].title,
          description: GROUP_META[id].description,
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

  const actualGroups = await readdir(OUTPUT_DIR).catch(() => [] as string[]);
  const missing = expectedGroups.filter((group) => !actualGroups.includes(group));

  if (missing.length > 0) {
    throw new Error(
      `Expected groups [${expectedGroups.join(', ')}] in ${OUTPUT_DIR}, missing: [${missing.join(', ')}]. Got: [${actualGroups.join(', ') || '<empty>'}]`,
    );
  }

  await Promise.all(
    expectedGroups.map((group) =>
      writeFile(
        path.join(OUTPUT_DIR, group, 'meta.json'),
        JSON.stringify(
          (({ title, navTitle, description }) => ({
            title: navTitle ?? title,
            description,
            pages: ['...'],
          }))(GROUP_META[group]),
          null,
          2,
        ) + '\n',
      ),
    ),
  );

  // fumadocs-openapi's index writer emits only title and description, so a group whose feature is
  // not in the latest release gets its comingSoon frontmatter added here. The docs page component
  // reads that flag to render the badge and the warning callout.
  const comingSoonPages = expectedGroups.flatMap((group) => {
    const { comingSoon, comingSoonOperations = [] } = GROUP_META[group];

    return [
      ...(comingSoon ? [path.join(OUTPUT_DIR, group, 'index.mdx')] : []),
      ...comingSoonOperations.map((operationId) =>
        path.join(OUTPUT_DIR, group, `${operationId}.mdx`),
      ),
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
