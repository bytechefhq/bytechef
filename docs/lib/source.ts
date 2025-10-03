import {
  type InferMetaType,
  type InferPageType,
  loader,
} from 'fumadocs-core/source';
import { openapiPlugin } from 'fumadocs-openapi/server';
import { icons } from 'lucide-react';
import { createElement } from 'react';
import { docs } from '@/.source';
// import { lucideIconsPlugin } from 'fumadocs-core/source/lucide-icons';

export const source = loader({
  baseUrl: '/',
  icon(icon) {
    if (!icon) {
      // You may set a default icon
      return;
    }

    if (icon in icons) return createElement(icons[icon as keyof typeof icons]);
  },
  source: docs.toFumadocsSource(),
  plugins: [/*lucideIconsPlugin(),*/ openapiPlugin()],
});

export type Page = InferPageType<typeof source>;
export type Meta = InferMetaType<typeof source>;
