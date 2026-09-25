import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SPECS_ROOT = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '../../../server/ee/libs',
);

/**
 * The public OpenAPI documents, keyed by the name they are published under: the reference pages
 * are generated from them, and /openapi/<name>.json serves them as-is plus the Cloud origin.
 */
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
  'automation-data-tables': path.join(
    SPECS_ROOT,
    'automation/automation-data-table/automation-data-table-public-rest/openapi.yaml',
  ),
  automation: path.join(
    SPECS_ROOT,
    'automation/automation-configuration/automation-configuration-public-rest/openapi.yaml',
  ),
  // Not a *-public-rest module: this one carries both openapi.yaml (public, bearer only) and
  // openapi-internal.yaml (session-authenticated, admin console). Wire the public one only.
  'custom-components': path.join(
    SPECS_ROOT,
    'platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-rest/openapi.yaml',
  ),
} as const;

export type SpecName = keyof typeof SPECS;

/** The spec published at /openapi.json: the documented, customer-facing embedded API. */
export const DEFAULT_SPEC: SpecName = 'embedded';

export function isSpecName(name: string): name is SpecName {
  return Object.hasOwn(SPECS, name);
}
