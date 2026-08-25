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
  'embedded-workflow-execution': {
    title: 'Embedded Workflow Executions',
    navTitle: 'Workflow Executions',
    description: "Fetch a tenant's workflow executions, and a single execution with its full detail.",
    // The public executions endpoints are on the upcoming release track, as their automation twin below.
    comingSoon: true,
  },
  'embedded-tool-invocation': {
    title: 'Embedded Tool Invocations',
    navTitle: 'Tool Invocations',
    description: "Fetch a connected user's tool and action invocation history.",
    comingSoon: true,
  },
  'embedded-configuration-automation-project-code-workflow': {
    title: 'Embedded Automation Bridge',
    navTitle: 'Automation Bridge',
    description:
      'List the catalog projects of the embedded automation bridge, and deploy a code-native automation project into it.',
    comingSoon: true,
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

  // GROUP_META stays keyed by a flat name, so a group keeps its entry wherever it is filed.
  const metaKeyOf = (schemaId: string, { tag, frontend }: Operation) =>
    [schemaId, taggedSchemas.has(schemaId) ? tag : null, frontend ? 'frontend' : null]
      .filter(Boolean)
      .join('-');

  // Ids double as output paths, so nesting them nests the sidebar: the embedded specs land under
  // `<audience>/`, so the sidebar carries a Backend and a Frontend folder under one separator. Other
  // specs stay flat until there is a second audience to separate them from.
  const idOf = (schemaId: string, operation: Operation) => {
    const group = [schemaId, taggedSchemas.has(schemaId) ? operation.tag : null]
      .filter(Boolean)
      .join('-');

    if (!schemaId.startsWith('embedded')) return metaKeyOf(schemaId, operation);

    return `${operation.frontend ? 'frontend' : 'backend'}/${group}`;
  };

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
    const seen = new Map<string, string>();

    for (const operation of operations.values()) {
      seen.set(idOf(schemaId, operation), metaKeyOf(schemaId, operation));
    }

    return [...seen].map(([id, metaKey]) => ({ id, metaKey, schemaId }));
  });

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
