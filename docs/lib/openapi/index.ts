import { createOpenAPI } from 'fumadocs-openapi/server';
import { SPECS } from './specs.ts';

export { SPECS } from './specs.ts';

export const openapi = createOpenAPI({
  input: SPECS,
  proxyUrl: '/api/proxy',
});
