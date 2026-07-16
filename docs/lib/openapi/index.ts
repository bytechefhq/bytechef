import { createOpenAPI } from 'fumadocs-openapi/server';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SPECS_ROOT = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '../../../server/ee/libs',
);

export const SPECS = {
  embedded: path.join(
    SPECS_ROOT,
    'embedded/embedded-execution/embedded-execution-public-rest/openapi.yaml',
  ),
  'embedded-configuration': path.join(
    SPECS_ROOT,
    'embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml',
  ),
  'embedded-webhook': path.join(
    SPECS_ROOT,
    'embedded/embedded-webhook/embedded-webhook-public-rest/openapi.yaml',
  ),
  automation: path.join(
    SPECS_ROOT,
    'automation/automation-configuration/automation-configuration-public-rest/openapi.yaml',
  ),
} as const;

export const openapi = createOpenAPI({
  input: async () => SPECS,
  proxyUrl: '/api/proxy',
});
