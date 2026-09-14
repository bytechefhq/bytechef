import {getMcpServersFilter} from '@/shared/components/mcp-server/mcpServersFilter';
import {describe, expect, it} from 'vitest';

const getFilter = (search: string, pageTitle: unknown) =>
    getMcpServersFilter({
        groupLabel: 'Projects',
        groupSearchParamName: 'projectId',
        pageTitle,
        searchParams: new URLSearchParams(search),
    });

describe('getMcpServersFilter', () => {
    it('defaults to all components', () => {
        expect(getFilter('', undefined)).toEqual({label: 'Components', value: 'All Components'});
    });

    it('names the selected component', () => {
        expect(getFilter('componentName=httpClient', 'HTTP Client')).toEqual({
            label: 'Components',
            value: 'HTTP Client',
        });
    });

    it('names the selected group entry', () => {
        expect(getFilter('projectId=7', 'AI Agent')).toEqual({label: 'Projects', value: 'AI Agent'});
    });

    it('names the selected tag', () => {
        expect(getFilter('tagId=3', 'billing')).toEqual({label: 'Tags', value: 'billing'});
    });

    it('reports an unknown selection when the title is not resolved', () => {
        expect(getFilter('tagId=3', undefined)).toEqual({label: 'Tags', value: 'Unknown'});
    });
});
