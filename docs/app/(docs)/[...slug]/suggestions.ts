import { OramaClient } from '@oramacloud/client';
import type { Suggestion } from '@/components/not-found';

const client = new OramaClient({
  endpoint: 'https://cloud.orama.run/v1/indexes/docs-fk97oe',
  api_key: '',
});

export async function getSuggestions(pathname: string): Promise<Suggestion[]> {
  const results = await client.search({
    term: pathname,
    mode: 'vector',
    groupBy: {
      properties: ['url'],
      maxResult: 1,
    },
  });

  if (!results?.groups) return [];

  return results.groups.map((group) => {
    const doc = group.result[0];

    return {
      id: doc.id,
      href: doc.document.url,
      title: doc.document.title,
    };
  });
}
