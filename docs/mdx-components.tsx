import defaultMdxComponents from 'fumadocs-ui/mdx';
import * as FilesComponents from 'fumadocs-ui/components/files';
import * as StepsComponents from 'fumadocs-ui/components/steps';
import * as TabsComponents from 'fumadocs-ui/components/tabs';
import type { MDXComponents } from 'mdx/types';
import type { ComponentProps, FC } from 'react';
import { Accordion, Accordions } from 'fumadocs-ui/components/accordion';
import { ImageZoom } from 'fumadocs-ui/components/image-zoom';
import * as icons from 'lucide-react';
import { ComingSoonBadge } from '@/components/coming-soon-badge';
import { EEBadge } from '@/components/ee-badge';
import { OpenAPIPage } from '@/components/openapi-page';

export function getMDXComponents(components?: MDXComponents) {
  return {
    ...(icons as unknown as MDXComponents),
    ...defaultMdxComponents,
    ...TabsComponents,
    ...FilesComponents,
    ...StepsComponents,
    Accordion,
    Accordions,
    ComingSoonBadge,
    EEBadge,
    ImageZoom,
    OpenAPIPage,
    // Every documentation image is a screenshot of dense product UI, so all of them are
    // click-to-zoom. Mapping `img` rather than rewriting pages keeps the markdown `![]()`
    // syntax — which is what `remarkImage` needs to resolve a co-located relative path into
    // a bundled import. A hand-written `<ImageZoom src="foo/bar.png">` bypasses that and
    // silently resolves against the page URL instead.
    img: ImageZoom as unknown as FC<ComponentProps<'img'>>,
    ...components,
  } satisfies MDXComponents;
}

declare global {
  type MDXProvidedComponents = ReturnType<typeof getMDXComponents>;
}
