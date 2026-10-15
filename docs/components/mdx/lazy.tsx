'use client';

import type * as ImageZoomModule from 'fumadocs-ui/components/image-zoom';
import type * as TwoslashModule from 'fumadocs-twoslash/ui';
import dynamic from 'next/dynamic';
import type { ComponentProps, FC } from 'react';

/*
 * MDX components that only some pages render. Every docs page is served by one route, so a static import
 * would put each of them in every page's first-load JavaScript; through `dynamic` their code is fetched
 * only by the pages that use them, and they still render on the server.
 *
 * `dynamic` is typed as returning a class or function component; MDX component maps accept only function
 * components, which is what it actually returns.
 */

export const ImageZoom = dynamic(() =>
  import('fumadocs-ui/components/image-zoom').then((module) => module.ImageZoom),
) as FC<ComponentProps<typeof ImageZoomModule.ImageZoom>>;

export const Popup = dynamic(() =>
  import('fumadocs-twoslash/ui').then((module) => module.Popup),
) as FC<ComponentProps<typeof TwoslashModule.Popup>>;

export const PopupContent = dynamic(() =>
  import('fumadocs-twoslash/ui').then((module) => module.PopupContent),
) as FC<ComponentProps<typeof TwoslashModule.PopupContent>>;

export const PopupTrigger = dynamic(() =>
  import('fumadocs-twoslash/ui').then((module) => module.PopupTrigger),
) as FC<ComponentProps<typeof TwoslashModule.PopupTrigger>>;
