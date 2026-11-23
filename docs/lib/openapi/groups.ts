import { readFile } from 'node:fs/promises';
import { parse as parseYaml } from 'yaml';
import { SPECS } from './specs.ts';

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

export const GROUP_META: Record<
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
  'embedded-external-invocation': {
    title: 'Embedded External Invocations',
    navTitle: 'External Invocations',
    description: "List a connected user's recorded external invocations.",
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
  'embedded-configuration-automation-project': {
    title: 'Embedded Workflow Catalog',
    navTitle: 'Workflow Catalog',
    description: 'List the catalog projects available to connected users.',
  },
  'embedded-configuration-automation-project-code-workflow': {
    title: 'Embedded Workflow Catalog Deployment',
    navTitle: 'Catalog Deployment',
    description: 'List the catalog projects and deploy a code-based automation project into the catalog.',
  },
  'embedded-configuration-integration-code-workflow': {
    title: 'Embedded Integration Deployment',
    navTitle: 'Integration Deployment',
    description: 'Deploy a code-based integration.',
  },
  'embedded-configuration-mcp-automation-workflow': {
    title: 'Embedded MCP Workflows',
    navTitle: 'MCP Workflows',
    description: "Turn a connected user's workflow on or off as a tool for AI agents.",
  },
  // One entry per tag in the automation spec -- see SPLIT_BY_TAG. The keys are `automation-<tag>`.
  'embedded-webhook-app-event-trigger': {
    title: 'Embedded App Events',
    navTitle: 'App Events',
    description: "Fire an App Event for a connected user from your backend, starting every one of their subscribed workflows.",
  },
  'embedded-webhook-request-trigger': {
    title: 'Embedded Request Trigger',
    navTitle: 'Request Trigger',
    description: "Execute a single workflow of a connected user from your backend through its Request trigger and return its result.",
  },
  'automation-project-code-workflow': {
    title: 'Automation Projects',
    navTitle: 'Projects',
    description: 'Deploy a code-based project.',
  },
  'automation-project-git': {
    title: 'Automation Git',
    navTitle: 'Git',
    description: "Pull a project from its configured git repository.",
    // Deliberately not comingSoon: settings/git-configuration is not flagged.
  },
  // One entry per tag in the automation-data-tables spec -- the keys are `automation-data-tables-<tag>`.
  'automation-data-tables-data-table': {
    title: 'Automation Data Tables',
    navTitle: 'Tables',
    description: 'Create, fetch, update and delete data tables and their columns.',
  },
  'automation-data-tables-data-table-row': {
    title: 'Automation Data Table Rows',
    navTitle: 'Rows',
    description:
      'Query, write, batch, import and export the rows of a data table, and upsert them by external ID.',
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
    description: "List, create, delete and reauthorize the signed-in connected user's connections.",
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
  'embedded-configuration-automation-project-frontend': {
    title: 'Embedded Workflow Catalog (Frontend)',
    navTitle: 'Workflow Catalog',
    description: "List the catalog projects available to the signed-in connected user.",
  },
  'embedded-configuration-mcp-automation-workflow-frontend': {
    title: 'Embedded MCP Workflows (Frontend)',
    navTitle: 'MCP Workflows',
    description: "Turn the signed-in user's workflow on or off as a tool for AI agents.",
  },
  'embedded-webhook-app-event-trigger-frontend': {
    title: 'Embedded App Events (Frontend)',
    navTitle: 'App Events',
    description: "Fire an App Event to start every one of a connected user's subscribed workflows.",
  },
  'embedded-webhook-request-trigger-frontend': {
    title: 'Embedded Request Trigger (Frontend)',
    navTitle: 'Request Trigger',
    description: "Execute a single workflow through its Request trigger and return its result.",
  },

  'custom-components': {
    title: 'Custom Components',
    description: 'Public REST API for deploying a custom component to the platform.',
  },
};

export interface Operation {
  tag: string;
  frontend: boolean;
}

/**
 * Reads the tag and audience of every operation in a spec, keyed `<METHOD> <path>` -- the only
 * identity fumadocs-openapi hands `groupBy`, which receives
 * `{ schemaId, info, item: { path, method } }` and neither tags nor security.
 */
export async function readOperations(schemaId: keyof typeof SPECS) {
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

export interface OperationGroup {
  id: string;
  metaKey: string;
  schemaId: string;
}

export async function planGroups() {
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

  // Each group remembers the spec it came from, because the generator's `context.generatedEntries` is keyed by
  // schema id rather than by the group ids groupBy produces.
  const groups: OperationGroup[] = Object.keys(SPECS).flatMap((schemaId) => {
    const operations = operationsBySchema.get(schemaId) ?? new Map<string, Operation>();
    const seen = new Map<string, string>();

    for (const operation of operations.values()) {
      seen.set(idOf(schemaId, operation), metaKeyOf(schemaId, operation));
    }

    return [...seen].map(([id, metaKey]) => ({ id, metaKey, schemaId }));
  });

  return { groupIdOf, groups, operationsBySchema };
}
