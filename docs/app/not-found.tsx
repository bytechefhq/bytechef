import DocsLayout from '@/app/(docs)/layout';
import { NotFound } from '@/components/not-found';

/**
 * Prerendered once and served with a real HTTP 404 for every path that is not a page. Wrapping
 * the docs layout keeps the sidebar, header and theme around it, exactly as on a docs page.
 */
export default function RootNotFound() {
  return (
    <DocsLayout params={Promise.resolve({})}>
      <NotFound getSuggestions={() => Promise.resolve([])} />
    </DocsLayout>
  );
}
