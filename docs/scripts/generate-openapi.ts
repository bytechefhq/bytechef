import { readdir, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { generateFiles } from 'fumadocs-openapi';
import { openapi, SPECS } from '../lib/openapi';

const OUTPUT_DIR = path.resolve(
  import.meta.dirname,
  '../content/docs/openapi/(generated)',
);

const GROUP_META: Record<keyof typeof SPECS, { title: string; description: string }> = {
  embedded: {
    title: 'Embedded',
    description: 'Public REST API for ByteChef Embedded (execution): run component actions and list per-user tools.',
  },
  'embedded-configuration': {
    title: 'Embedded Configuration',
    description: 'Public REST API for ByteChef Embedded (configuration): manage integrations, instances, and end-user workflows.',
  },
  'embedded-webhook': {
    title: 'Embedded Webhook',
    description: 'Public REST API for ByteChef Embedded (webhook): fire App Events and trigger workflows synchronously via Request triggers.',
  },
  automation: {
    title: 'Automation',
    description: 'Public REST API for ByteChef Automation (code-based projects).',
  },
};

await rm(OUTPUT_DIR, { recursive: true, force: true });

await generateFiles({
  input: openapi,
  output: OUTPUT_DIR,
  per: 'operation',
  groupBy: (entry) => entry.schemaId,
});

const expectedGroups = Object.keys(SPECS);
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
      JSON.stringify({ ...GROUP_META[group as keyof typeof SPECS], pages: ['...'] }, null, 2) + '\n',
    ),
  ),
);

console.log(`OpenAPI pages generated under ${OUTPUT_DIR} (groups: ${expectedGroups.join(', ')})`);
