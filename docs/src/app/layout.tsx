import './global.css';
import { RootProvider } from 'fumadocs-ui/provider';
import { Inter } from 'next/font/google';
import type { ReactNode } from 'react';
import {baseUrl, createMetadata} from "@/lib/metadata";
import {PostHogProvider} from "@/app/providers";

export const metadata = createMetadata({
  title: {
    template: '%s | ByteChef',
    default: 'ByteChef',
  },
  description: 'ByteChef is an open-source, enterprise-ready platform for building AI agents, automating workflows, and integrating applications across SaaS, APIs, and databases with flexible deployment.',
  metadataBase: baseUrl,
});

const inter = Inter({
  subsets: ['latin'],
});

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" className={inter.className} suppressHydrationWarning>
      <body className="flex flex-col min-h-screen">
        <PostHogProvider>
          <RootProvider>{children}</RootProvider>
        </PostHogProvider>
      </body>
    </html>
  );
}
