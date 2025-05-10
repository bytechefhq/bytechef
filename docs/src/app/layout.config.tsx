import type { BaseLayoutProps } from 'fumadocs-ui/layouts/shared';
import logo from './assets/logo.svg';
import Image from "next/image";

/**
 * Shared layout configurations
 *
 * you can customise layouts individually from:
 * Home Layout: app/(home)/layout.tsx
 * Docs Layout: app/docs/layout.tsx
 */
export const baseOptions: BaseLayoutProps = {
  nav: {
    title: (
      <>
        <Image
          priority
          src={logo}
          alt="ByteChef Logo"
          height={24}
        />
        ByteChef
      </>
    ),
  },
  githubUrl: 'https://github.com/bytechefhq/bytechef',
  links: [
    // {
    //   text: 'Discord',
    //   url: 'https://discord.com/invite/JcNSqJ7vK8',
    //   active: 'nested-url',
    // },
    // {
    //   text: 'GitHub',
    //   url: 'https://github.com/bytechefhq/bytechef',
    //   active: 'nested-url',
    // },
    // {
    //   text: 'Twitter',
    //   url: 'https://x.com/bytechefhq',
    //   active: 'nested-url',
    // }
  ],
};
