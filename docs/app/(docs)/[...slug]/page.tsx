import type { Metadata } from 'next';
import { type ComponentProps, type FC, type ReactNode } from 'react';
import * as Twoslash from 'fumadocs-twoslash/ui';
import { Callout } from 'fumadocs-ui/components/callout';
import { TypeTable } from 'fumadocs-ui/components/type-table';
import * as Preview from '@/components/preview';
import { createMetadata, getPageImage } from '@/lib/metadata';
import { source } from '@/lib/source';
import { Wrapper } from '@/components/preview/wrapper';
import { Mermaid } from '@/components/mdx/mermaid';
import { ComingSoonBadge } from '@/components/coming-soon-badge';
import { EEBadge } from '@/components/ee-badge';
import { Feedback } from '@/components/feedback';
import { onRateAction, owner, repo } from '@/lib/github';
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from '@/components/ui/hover-card';
import Link from 'fumadocs-core/link';
import { getPageTreePeers } from 'fumadocs-core/page-tree';
import { Card, Cards } from 'fumadocs-ui/components/card';
import { getMDXComponents } from '@/mdx-components';
import { LLMCopyButton, ViewOptions } from '@/components/ai/page-actions';
import { Banner } from 'fumadocs-ui/components/banner';
import { Installation } from '@/components/preview/installation';
import { Customisation } from '@/components/preview/customisation';
import { DocsPage, PageLastUpdate } from 'fumadocs-ui/layouts/docs/page';
import { NotFound } from '@/components/not-found';
// import { getSuggestions } from '@/app/(docs)/[...slug]/suggestions';
import { PathUtils } from 'fumadocs-core/source';

function PreviewRenderer({ preview }: { preview: string }): ReactNode {
  if (preview && preview in Preview) {
    const Comp = Preview[preview as keyof typeof Preview];
    return <Comp />;
  }

  return null;
}

export const revalidate = false;

export default async function Page(props: PageProps<'/[...slug]'>) {
  const params = await props.params;
  const page = source.getPage(params.slug);

  if (!page)
    return (
      // <NotFound getSuggestions={() => getSuggestions(params.slug.join(' '))} />
      <NotFound getSuggestions={() => Promise.resolve([])} />
    );

  const { body: Mdx, toc, lastModified } = await page.data.load();

  // Generated OpenAPI operation pages carry `full: true`. Passing it through is what gives
  // fumadocs-openapi room for its two-column reference layout — request and parameters on the
  // left, code samples and response examples on the right. Without it the page renders in the
  // constrained article width beside a TOC column, and those right-hand panels collapse below
  // the fold, which reads as "the API page is not interactive".
  const isFullWidth = page.data.full === true;

  return (
    <DocsPage
      full={isFullWidth}
      toc={toc}
      tableOfContent={{
        enabled: !isFullWidth,
        style: 'clerk',
      }}
    >
      {(page.data.ee || page.data.comingSoon) && (
        <div className="flex flex-row flex-wrap items-center gap-2 -mb-2">
          {page.data.ee && <EEBadge />}
          {page.data.comingSoon && <ComingSoonBadge />}
        </div>
      )}
      <h1 className="text-[1.75em] font-semibold">{page.data.title}</h1>
      <p className="text-lg text-fd-muted-foreground mb-2">
        {page.data.description}
      </p>
      {page.data.comingSoon && (
        <Callout type="warn" title="Coming soon" className="not-prose mb-4">
          This capability is not available in the latest released version of
          ByteChef.
        </Callout>
      )}
      <div className="flex flex-row flex-wrap gap-2 items-center border-b pb-6">
        <LLMCopyButton markdownUrl={`${page.url}.mdx`} />
        <ViewOptions
          markdownUrl={`${page.url}.mdx`}
          githubUrl={`https://github.com/${owner}/${repo}/blob/master/docs/content/docs/${page.path}`}
        />
      </div>
      <div
        className={
          isFullWidth ? 'flex-1' : 'prose flex-1 text-fd-foreground/90'
        }
      >
        {page.data.preview && <PreviewRenderer preview={page.data.preview} />}
        <Mdx
          components={getMDXComponents({
            ...Twoslash,
            a: ({ href, ...props }) => {
              const found = source.getPageByHref(href ?? '', {
                dir: PathUtils.dirname(page.path),
              });

              if (!found) return <Link href={href} {...props} />;

              return (
                <HoverCard>
                  <HoverCardTrigger
                    href={
                      found.hash
                        ? `${found.page.url}#${found.hash}`
                        : found.page.url
                    }
                    {...props}
                  >
                    {props.children}
                  </HoverCardTrigger>
                  <HoverCardContent className="text-sm">
                    <p className="font-medium">{found.page.data.title}</p>
                    <p className="text-fd-muted-foreground">
                      {found.page.data.description}
                    </p>
                  </HoverCardContent>
                </HoverCard>
              );
            },
            Banner,
            Mermaid,
            TypeTable,
            Wrapper,
            blockquote: Callout as unknown as FC<ComponentProps<'blockquote'>>,
            DocsCategory: ({ url }) => {
              return <DocsCategory url={url ?? page.url} />;
            },
            Installation,
            Customisation,
          })}
        />
        {page.data.index ? <DocsCategory url={page.url} /> : null}
      </div>
      <Feedback onRateAction={onRateAction} />
      {lastModified && <PageLastUpdate date={lastModified} />}
    </DocsPage>
  );
}

function DocsCategory({ url }: { url: string }) {
  return (
    <Cards>
      {getPageTreePeers(source.pageTree, url).map((peer) => (
        <Card key={peer.url} title={peer.name} href={peer.url}>
          {peer.description}
        </Card>
      ))}
    </Cards>
  );
}

export async function generateMetadata(
  props: PageProps<'/[...slug]'>,
): Promise<Metadata> {
  const { slug = [] } = await props.params;
  const page = source.getPage(slug);
  if (!page)
    return createMetadata({
      title: 'Not Found',
    });

  const description =
    page.data.description ?? 'The platform for building ai-driven automations';

  const image = {
    url: getPageImage(page).url,
    width: 1200,
    height: 630,
  };

  return createMetadata({
    title: page.data.title,
    description,
    openGraph: {
      url: `/${page.slugs.join('/')}`,
      images: [image],
    },
    twitter: {
      images: [image],
    },
  });
}

export function generateStaticParams() {
  return source.generateParams();
}
