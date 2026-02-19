import {
  type InferMetaType,
  type InferPageType,
  type LoaderPlugin,
  loader,
  multiple,
} from 'fumadocs-core/source';
import { openapiPlugin, openapiSource } from 'fumadocs-openapi/server';
import { docs } from 'fumadocs-mdx:collections/server';
import { lucideIconsPlugin } from 'fumadocs-core/source/lucide-icons';
import { openapi } from '@/lib/openapi';

export const source = loader(
  multiple({
    docs: docs.toFumadocsSource(),
    openapi: await openapiSource(openapi, {
      baseDir: 'openapi/(generated)',
    }),
  }),
  {
    baseUrl: '/',
    plugins: [pageTreeCodeTitles(), featureFlagsPlugin(), lucideIconsPlugin(), openapiPlugin()],
  },
);

const FEATURE_FLAG_PAGES: Record<string, string> = {
  '/platform/copilot': 'ff-1570',
};

function getEnabledFeatureFlags(): Set<string> {
  const flags = new Set<string>();

  for (const [key, value] of Object.entries(process.env)) {
    if (key.startsWith('BYTECHEF_FEATUREFLAGS_') && value) {
      flags.add(value);
    }
  }

  return flags;
}

function featureFlagsPlugin(): LoaderPlugin {
  const enabledFlags = getEnabledFeatureFlags();

  return {
    transformPageTree: {
      folder(node) {
        const filtered = node.children.filter((child) => {
          if (child.type !== 'page') return true;

          const requiredFlag = FEATURE_FLAG_PAGES[child.url];

          return !requiredFlag || enabledFlags.has(requiredFlag);
        });

        return {...node, children: filtered};
      },
    },
  };
}

function pageTreeCodeTitles(): LoaderPlugin {
  return {
    transformPageTree: {
      file(node) {
        if (
          typeof node.name === 'string' &&
          (node.name.endsWith('()') || node.name.match(/^<\w+ \/>$/))
        ) {
          return {
            ...node,
            name: <code className="text-[0.8125rem]">{node.name}</code>,
          };
        }
        return node;
      },
    },
  };
}

export type Page = InferPageType<typeof source>;
export type Meta = InferMetaType<typeof source>;
