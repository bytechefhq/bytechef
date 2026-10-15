'use client';

import dynamic from 'next/dynamic';

/**
 * Every docs page shares one route, so a statically imported client component ships on all of them. The
 * OpenAPI renderer (and the Shiki grammars and markdown pipeline it pulls in) is only rendered on the API
 * reference pages; loading it through `dynamic` keeps it out of every other page's first-load JavaScript
 * while still rendering it on the server.
 */
export const APIPage = dynamic(() =>
  import('@/components/api-page.impl').then((module) => module.APIPage),
);
