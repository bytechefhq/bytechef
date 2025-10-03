import { docs } from '@/.source';
import type { InferMetaType, InferPageType } from 'fumadocs-core/source';
import { loader } from 'fumadocs-core/source';
import { icons } from 'lucide-react';
import { createElement } from 'react';
// import { attachFile, createOpenAPI } from 'fumadocs-openapi/server';

// See https://fumadocs.vercel.app/docs/headless/source-api for more info
export const source = loader({
  // it assigns a URL to your pages
  baseUrl: '/',
  icon(icon) {
    if (!icon) {
      // You may set a default icon
      return;
    }

    if (icon in icons) return createElement(icons[icon as keyof typeof icons]);
  },
  source: docs.toFumadocsSource(),
  // pageTree: {
  //   attachFile,
  // },
});

// export const openapi = createOpenAPI({
//   proxyUrl: '/api/proxy',
//   shikiOptions: {
//     themes: {
//       dark: 'vesper',
//       light: 'vitesse-light',
//     },
//   },
// });

export type Page = InferPageType<typeof source>;
export type Meta = InferMetaType<typeof source>;
