import type { Metadata } from 'next';
import { type ComponentProps, type FC, type ReactNode } from 'react';
import { Popup, PopupContent, PopupTrigger } from '@/components/mdx/lazy';
import { Callout } from 'fumadocs-ui/components/callout';
import { TypeTable } from 'fumadocs-ui/components/type-table';
import * as Preview from '@/components/preview';
import { baseUrl, createMetadata, getPageImage } from '@/lib/metadata';
import {
  fitDescription,
  getDescriptionFillers,
  getDistinguishingSlugs,
  getReferenceBaseName,
  getSeoTitle,
  humanizeSlug,
  MAX_TITLE_LENGTH,
} from '@/lib/seo';
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
// import { getSuggestions } from '@/app/(docs)/[...slug]/suggestions';
import { PathUtils } from 'fumadocs-core/source';
import { notFound } from 'next/navigation';

function PreviewRenderer({ preview }: { preview: string }): ReactNode {
  if (preview && preview in Preview) {
    const Comp = Preview[preview as keyof typeof Preview];
    return <Comp />;
  }

  return null;
}

export const revalidate = false;

// Every page comes from generateStaticParams, so an unknown slug is a 404 served from the
// prerendered not-found page. Rendering unknown slugs on demand would answer them with a
// client-rendered shell whose HTML body is empty, which agents cannot read.
export const dynamicParams = false;

export default async function Page(props: PageProps<'/[...slug]'>) {
  const params = await props.params;
  const page = source.getPage(params.slug);

  // A real HTTP 404, rendered by app/(docs)/not-found.tsx inside the docs layout. Returning the
  // NotFound component from here would answer 200 and make agents believe every path exists.
  if (!page) notFound();

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
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(getStructuredData(page, lastModified)),
        }}
      />
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
            Popup,
            PopupContent,
            PopupTrigger,
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

  const title = getPageSeoTitle(page);
  const description = getPageSeoDescription(page);

  const image = {
    url: getPageImage(page).url,
    width: 1200,
    height: 630,
  };

  // The layout template appends " | ByteChef"; skip it when it would push the title past ~70 characters.
  const fitsSuffix = `${title} | ByteChef`.length <= MAX_TITLE_LENGTH;

  return createMetadata({
    title: fitsSuffix ? title : { absolute: title },
    description,
    alternates: {
      canonical: new URL(page.url, baseUrl).toString(),
    },
    openGraph: {
      title,
      type: 'article',
      url: page.url,
      images: [image],
    },
    twitter: {
      title,
      images: [image],
    },
  });
}

type DocsPageType = NonNullable<ReturnType<typeof source.getPage>>;

function getPageSeoTitle(page: DocsPageType): string {
  const pages = source.getPages();
  const parentSlugs = page.slugs.slice(0, -1).join('/');
  const baseName = getReferenceBaseName(page.slugs.at(-1) ?? '');

  return getSeoTitle({
    distinguishingSlugs: getDistinguishingSlugs(
      page.slugs,
      pages
        .filter(
          (candidate) =>
            candidate.url !== page.url && candidate.data.title === page.data.title,
        )
        .map((candidate) => candidate.slugs),
    ),
    slugs: page.slugs,
    title: page.data.title,
    versionCount: pages.filter(
      (candidate) =>
        candidate.slugs.slice(0, -1).join('/') === parentSlugs &&
        getReferenceBaseName(candidate.slugs.at(-1) ?? '') === baseName,
    ).length,
  });
}

function getPageSeoDescription(page: DocsPageType): string {
  // OpenAPI fillers leave the title out, so a page without a description starts from its title instead.
  const description =
    page.data.description ||
    (page.slugs[0] === 'openapi' ? `${page.data.title}.` : undefined);

  return fitDescription(description, getDescriptionFillers(page.slugs, page.data.title));
}

function getStructuredData(page: DocsPageType, lastModified?: Date) {
  const pageUrl = new URL(page.url, baseUrl).toString();

  const breadcrumbs = page.slugs.map((_, index) => {
    const slugs = page.slugs.slice(0, index + 1);
    const crumbPage = source.getPage(slugs);

    return {
      '@type': 'ListItem',
      position: index + 1,
      name: crumbPage?.data.title ?? humanizeSlug(slugs[index]),
      item: new URL(`/${slugs.join('/')}`, baseUrl).toString(),
    };
  });

  return [
    {
      '@context': 'https://schema.org',
      '@type': 'TechArticle',
      headline: page.data.title,
      description: getPageSeoDescription(page),
      url: pageUrl,
      mainEntityOfPage: pageUrl,
      ...(lastModified && { dateModified: new Date(lastModified).toISOString() }),
      publisher: {
        '@type': 'Organization',
        name: 'ByteChef',
        url: 'https://www.bytechef.io',
        logo: new URL('/logo.svg', baseUrl).toString(),
      },
    },
    {
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: breadcrumbs,
    },
  ];
}

export function generateStaticParams() {
  return source.generateParams();
}
