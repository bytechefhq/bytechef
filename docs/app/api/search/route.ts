import { source } from '@/lib/source';
import { createSearchAPI } from 'fumadocs-core/search/server';

export const { GET } = createSearchAPI('advanced', {
  language: 'english',
  indexes: async () => {
    const pages = source.getPages();
    const indexes = await Promise.all(
      pages.map(async (page) => {
        if (page.data.type === 'openapi') return undefined;

        const loaded = await page.data.load();

        return {
          title: page.data.title,
          description: page.data.description,
          url: page.url,
          id: page.url,
          structuredData: loaded.structuredData,
        };
      }),
    );

    return indexes.filter((v): v is NonNullable<typeof v> => v !== undefined);
  },
});
