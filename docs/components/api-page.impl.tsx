'use client';

import { createOpenAPIPage } from 'fumadocs-openapi/ui';

export const APIPage = createOpenAPIPage({
  shikiOptions: {
    themes: {
      dark: 'vesper',
      light: 'vitesse-light',
    },
  },
  mediaAdapters: {
    'text/csv': {
      encode: (data) => data.body as BodyInit,
      generateExample: () => undefined,
    },
  },
});
