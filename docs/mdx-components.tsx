import defaultMdxComponents from 'fumadocs-ui/mdx';
import * as FilesComponents from 'fumadocs-ui/components/files';
import * as TabsComponents from 'fumadocs-ui/components/tabs';
import type { MDXComponents } from 'mdx/types';
import { Accordion, Accordions } from 'fumadocs-ui/components/accordion';
import * as icons from 'lucide-react';
import { EEBadge } from '@/components/ee-badge';

export function getMDXComponents(components?: MDXComponents) {
  return {
    ...(icons as unknown as MDXComponents),
    ...defaultMdxComponents,
    ...TabsComponents,
    ...FilesComponents,
    Accordion,
    Accordions,
    EEBadge,
    ...components,
  } satisfies MDXComponents;
}

declare global {
  type MDXProvidedComponents = ReturnType<typeof getMDXComponents>;
}
