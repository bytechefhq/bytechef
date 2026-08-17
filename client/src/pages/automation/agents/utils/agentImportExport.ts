import {ExportAiAgentDocument, ExportAiAgentQuery, ExportAiAgentQueryVariables} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';

/**
 * Downloads an agent's configuration as a JSON file.
 *
 * Fetched imperatively rather than through `useExportAiAgentQuery`: this runs on a menu click, and a query hook
 * would either fetch on every render of the menu or need an `enabled` flag toggled just to fire once — the same
 * reason `tasks.api.ts` reaches for the generated fetcher directly.
 *
 * The file is named after the agent so a folder of exports stays readable; the id is not in the name because an
 * export is not tied to the row it came from — importing it creates a new agent.
 */
export const exportAgent = async (id: string, title: string): Promise<void> => {
    const data = await fetcher<ExportAiAgentQuery, ExportAiAgentQueryVariables>(ExportAiAgentDocument, {id})();

    const blob = new Blob([data.exportAiAgent], {type: 'application/json'});
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');

    link.download = `${slugify(title)}.json`;
    link.href = url;

    link.click();

    // The object URL holds the blob alive until it is revoked, and nothing else references it once the download
    // has started.
    URL.revokeObjectURL(url);
};

const slugify = (title: string): string =>
    title
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '') || 'agent';

export default exportAgent;
