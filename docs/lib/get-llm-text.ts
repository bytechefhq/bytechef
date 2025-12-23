import { type Page } from '@/lib/source';

export async function getLLMText(page: Page) {
  if (page.data.type === 'openapi') return '';

  const category =
    {
      automation: 'ByteChef Automation',
      'developer-guide': 'ByteChef Developer Guide',
      embedded: 'ByteChef Embedded',
      reference: 'ByteChef Reference',
    }[page.slugs[0]] ?? page.slugs[0];

  const processed = await page.data.getText('processed');

  return `# ${category}: ${page.data.title}
URL: ${page.url}
Source: https://raw.githubusercontent.com/bytechefhq/bytechef/refs/heads/main/apps/docs/content/docs/${page.path}

${page.data.description ?? ''}

${processed}`;
}
