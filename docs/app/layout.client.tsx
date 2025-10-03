'use client';

import { useParams } from 'next/navigation';
import { type ReactNode } from 'react';
import { cn } from '@/lib/cn';
import Logo from '@/public/logo.svg';
import Image from "next/image";

export function Body({
  children,
}: {
  children: ReactNode;
}): React.ReactElement {
  const mode = useMode();

  return (
    <body className={cn(mode, 'relative flex min-h-screen flex-col')}>
      {children}
    </body>
  );
}

export function useMode(): string | undefined {
  const { slug } = useParams();
  return Array.isArray(slug) && slug.length > 0 ? slug[0] : undefined;
}

export function LogoIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <Image
      priority
      src={Logo}
      alt="ByteChef Logo"
      className={props.className}
      height={24}
    />
  );
}
