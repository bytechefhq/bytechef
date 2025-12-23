import type { Metadata } from 'next/types';
import { Page } from './source';

export function createMetadata(override: Metadata): Metadata {
  return {
    ...override,
    openGraph: {
      title: override.title ?? undefined,
      description: override.description ?? undefined,
      url: 'https://docs.bytechef.io',
      images: '/banner.png',
      siteName: 'ByteChef',
      ...override.openGraph,
    },
    twitter: {
      card: 'summary_large_image',
      creator: '@bytechefhq',
      title: override.title ?? undefined,
      description: override.description ?? undefined,
      images: '/banner.png',
      ...override.twitter,
    },
    // alternates: {
    //   types: {
    //     'application/rss+xml': [
    //       {
    //         title: 'ByteChef Blog',
    //         url: 'https://blog.bytechef.io/rss.xml',
    //       },
    //     ],
    //   },
    //   ...override.alternates,
    // },
  };
}

export function getPageImage(page: Page) {
  const segments = [...page.slugs, 'image.webp'];

  return {
    segments,
    url: `/og/${segments.join('/')}`,
  };
}

export const baseUrl =
  process.env.NODE_ENV === 'development'
    ? new URL('http://localhost:3000')
    : new URL('https://docs.bytechef.io');
