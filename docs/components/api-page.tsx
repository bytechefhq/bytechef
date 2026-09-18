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
    // fumadocs-openapi ships no default adapter for `text/csv` (used by the data table
    // import/export operations), so an unhandled media type throws at build time. It's a plain
    // string body, so pass it through raw the same way `application/octet-stream` does, with no
    // generated code example.
    'text/csv': {
      encode: (data) => data.body as BodyInit,
      generateExample: () => undefined,
    },
  },
});
