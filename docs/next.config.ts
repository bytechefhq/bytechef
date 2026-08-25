import createBundleAnalyzer from '@next/bundle-analyzer';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { createMDX } from 'fumadocs-mdx/next';
import { PHASE_DEVELOPMENT_SERVER, PHASE_PRODUCTION_BUILD } from 'next/constants.js';
import type { NextConfig } from 'next';

const withAnalyzer = createBundleAnalyzer({
  enabled: process.env.ANALYZE === 'true',
});

const config: NextConfig = {
  reactStrictMode: true,
  // Next 16.3 writes docs/AGENTS.md + docs/CLAUDE.md on `next dev`; agent instructions for this
  // repo live in the root CLAUDE.md, so keep the generated pair out of the tree.
  agentRules: false,
  logging: {
    fetches: {
      fullUrl: true,
    },
  },
  serverExternalPackages: [
    'ts-morph',
    'typescript',
    'oxc-transform',
    'twoslash',
    'shiki',
    '@takumi-rs/image-response',
  ],
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'avatars.githubusercontent.com',
        port: '',
      },
      {
        protocol: 'https',
        hostname: 'static.scarf.sh',
        pathname: '/**',
      },
    ],
  },
  async rewrites() {
    return [
      {
        source: '/:path*.mdx',
        destination: '/llms.mdx/:path*',
      },
    ];
  },
  async redirects() {
    return [
      {
        source: '/',
        destination: '/platform',
        permanent: false,
      },
      {
        source: '/introduction',
        destination: '/platform',
        permanent: false,
      },
      {
        source: '/automation/personal-agents',
        destination: '/automation/tasks',
        permanent: false,
      },
      {
        // Copilot is titled AI Copilot everywhere; the route follows.
        source: '/platform/automation/build/copilot',
        destination: '/platform/automation/build/ai-copilot',
        permanent: true,
      },
      {
        // Deploy Workflows became simply Workflows inside the Deploy group.
        source: '/platform/automation/deploy/deploy-workflows',
        destination: '/platform/automation/deploy/workflows',
        permanent: true,
      },
      {
        // Copilot moved up out of the retired "With AI" group.
        source: '/platform/automation/build/with-ai/copilot',
        destination: '/platform/automation/build/ai-copilot',
        permanent: true,
      },
      {
        // Tenant-Isolated Security merged into the White-Label Execution page.
        source: '/platform/embedded/get-started/tenant-isolated-security',
        destination:
          '/platform/embedded/get-started/white-label-execution#tenant-isolated-security',
        permanent: true,
      },
      {
        source: '/platform/enterprise/collaboration-devops',
        destination: '/platform/enterprise#collaboration-and-devops',
        permanent: true,
      },
      {
        source: '/platform/enterprise/collaboration-devops/build-once-deploy-many',
        destination: '/platform/automation/deploy/deploy-workflows',
        permanent: true,
      },
      {
        source: '/platform/enterprise/collaboration-devops/environments',
        destination: '/platform/automation/deploy/environments',
        permanent: true,
      },
      {
        source: '/platform/enterprise/collaboration-devops/git-backed-change-tracking',
        destination: '/platform/automation/settings/git-configuration',
        permanent: true,
      },
      {
        source: '/platform/enterprise/collaboration-devops/workflow-executions',
        destination: '/platform/automation/monitor/workflow-executions',
        permanent: true,
      },
      {
        source: '/platform/enterprise/collaboration-devops/workflow-versioning',
        destination: '/platform/automation/deploy/deploy-workflows',
        permanent: true,
      },
      {
        source: '/platform/enterprise/collaboration-devops/workspaces-projects',
        destination: '/platform/settings/workspaces',
        permanent: true,
      },
      {
        source: '/platform/enterprise/data-knowledge',
        destination: '/platform/enterprise#data-and-knowledge',
        permanent: true,
      },
      {
        source: '/platform/enterprise/data-knowledge/data-tables',
        destination: '/platform/automation/data/data-tables',
        permanent: true,
      },
      {
        source: '/platform/enterprise/data-knowledge/embedding-models',
        destination: '/platform/settings/ai-providers',
        permanent: true,
      },
      {
        source: '/platform/enterprise/data-knowledge/knowledge-base',
        destination: '/platform/automation/data/knowledge-base',
        permanent: true,
      },
      {
        source: '/platform/enterprise/embedded-ipaas',
        destination: '/platform/embedded/get-started',
        permanent: true,
      },
      {
        source: '/platform/enterprise/extensibility',
        destination: '/platform/enterprise#extensibility',
        permanent: true,
      },
      {
        source: '/platform/enterprise/extensibility/api-connectors',
        destination: '/platform/settings/components/api-connectors',
        permanent: true,
      },
      {
        source: '/platform/enterprise/extensibility/built-in-components',
        destination: '/platform/automation/build/workflows/components',
        permanent: true,
      },
      {
        source: '/platform/enterprise/extensibility/code-workflows',
        destination: '/platform/automation/build/workflows/code-workflows',
        permanent: true,
      },
      {
        source: '/platform/enterprise/extensibility/custom-components',
        destination: '/platform/settings/components/custom-components',
        permanent: true,
      },
      {
        source: '/platform/enterprise/extensibility/mcp-integration',
        destination: '/platform/automation/deploy/mcp-servers',
        permanent: true,
      },
      {
        source: '/platform/enterprise/extensibility/polyglot-scripting',
        destination: '/platform/automation/build/workflows/code-workflows',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security',
        destination: '/platform/enterprise#governance-and-security',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/ai-guardrails',
        destination: '/platform/automation/settings/ai-agents/guardrails',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/api-keys',
        destination: '/platform/settings/admin-api-keys',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/audit-log',
        destination: '/platform/settings/audit-events',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/component-policies',
        destination: '/platform/settings/components/component-visibility',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/connection-visibility',
        destination: '/platform/settings/connections',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/data-retention',
        destination: '/platform/use-bytechef/self-hosted/configuration',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/encrypted-credentials',
        destination: '/platform/use-bytechef/self-hosted/configuration',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/flexible-deployment',
        destination: '/platform/use-bytechef/self-hosted',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/license-gated-distribution',
        destination: '/platform/settings/license',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/oauth2-clients',
        destination: '/platform/settings/oauth2-clients',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/observability',
        destination: '/platform/use-bytechef/self-hosted/management/observability',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/rbac',
        destination: '/platform/settings/users',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/sso',
        destination: '/platform/settings/identity-providers',
        permanent: true,
      },
      {
        source: '/platform/enterprise/governance-security/users',
        destination: '/platform/settings/users',
        permanent: true,
      },
      {
        source: '/platform/enterprise/runtime-job-runner',
        destination: '/platform/use-bytechef/self-hosted/runtime-job',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability',
        destination: '/platform/enterprise#scale-and-reliability',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/cloud-native-storage',
        destination: '/platform/use-bytechef/self-hosted/configuration/file-storage',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/crash-recovery',
        destination: '/platform/use-bytechef/self-hosted/management/crash-recovery',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/distributed-scheduler',
        destination: '/platform/use-bytechef/self-hosted/installation/distributed',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/horizontal-scaling',
        destination: '/platform/use-bytechef/self-hosted/installation/distributed',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/message-brokers',
        destination: '/platform/use-bytechef/self-hosted/configuration/message-brokers',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/multi-tenant-isolation',
        destination: '/platform/use-bytechef/self-hosted/architecture',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/plan-limits',
        destination: '/platform/use-bytechef/self-hosted/configuration/plan-limits',
        permanent: true,
      },
      {
        source: '/platform/enterprise/scale-reliability/runtime-job',
        destination: '/platform/use-bytechef/self-hosted/runtime-job',
        permanent: true,
      },
      {
        source: '/platform/enterprise/support-trust',
        destination: '/platform/enterprise',
        permanent: true,
      },
      {
        source: '/platform/enterprise/support-trust/production-migrations',
        destination: '/platform/use-bytechef/self-hosted/management/upgrades',
        permanent: true,
      },
      {
        source: '/platform/enterprise/support-trust/source-available-code',
        destination: '/platform/enterprise',
        permanent: true,
      },
      {
        source: '/platform/enterprise/support-trust/support-slas',
        destination: '/platform/enterprise',
        permanent: true,
      },
    ];
  },
};

const withMDX = createMDX();

/**
 * Writes the API reference pages under `content/docs/openapi/`, which are generated from the
 * monorepo's OpenAPI specs and left untracked.
 *
 * This runs from the config rather than from a `prebuild` script because the pages are only ever
 * as present as the command the host happens to run: a build command of `next build` skips every
 * npm script, and the site still builds -- fumadocs drops nav entries pointing at directories that
 * do not exist, so the whole reference section disappears without a single error. Generating here
 * cannot be skipped, and a failure fails the build.
 *
 * Only while building or serving in dev: `next start` loads this file too, and the runtime
 * filesystem is read-only.
 */
function generateOpenApiPages() {
  // Next loads this file again in each build worker, and a worker inherits this environment: without
  // the guard the tree would be cleared and rewritten underneath a worker already reading it.
  if (process.env.BYTECHEF_DOCS_OPENAPI_GENERATED === '1') return;

  process.env.BYTECHEF_DOCS_OPENAPI_GENERATED = '1';

  execFileSync(
    process.execPath,
    ['--experimental-strip-types', path.join(import.meta.dirname, 'scripts/generate-openapi.mts')],
    { stdio: 'inherit' },
  );
}

export default function nextConfig(phase: string) {
  if (phase === PHASE_PRODUCTION_BUILD || phase === PHASE_DEVELOPMENT_SERVER) {
    generateOpenApiPages();
  }

  return withAnalyzer(withMDX(config));
}
