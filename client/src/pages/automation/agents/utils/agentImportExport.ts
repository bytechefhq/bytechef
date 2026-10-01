import {ExportAiAgentDocument, ExportAiAgentQuery, ExportAiAgentQueryVariables} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';

export const exportAgent = async (id: string, title: string): Promise<void> => {
    const data = await fetcher<ExportAiAgentQuery, ExportAiAgentQueryVariables>(ExportAiAgentDocument, {id})();

    const blob = new Blob([data.exportAiAgent], {type: 'application/json'});
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');

    link.download = `${slugify(title)}.json`;
    link.href = url;

    link.click();

    URL.revokeObjectURL(url);
};

const slugify = (title: string): string =>
    title
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '') || 'agent';

export default exportAgent;
