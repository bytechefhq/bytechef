import useIntegrationComponentElements from '@/ee/pages/embedded/integrations/components/integration-list/useIntegrationComponentElements';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    useGetComponentDefinitionQuery: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: hoisted.useGetComponentDefinitionQuery,
}));

const queryResult = (data?: object) => ({data, isError: false, isLoading: false, refetch: vi.fn()});

describe('useIntegrationComponentElements', () => {
    beforeEach(() => {
        hoisted.useGetComponentDefinitionQuery.mockReset();
    });

    it('maps actions and tools sorted by title, naming tools like the MCP server', () => {
        hoisted.useGetComponentDefinitionQuery.mockReturnValue(
            queryResult({
                actions: [
                    {description: 'Sends', name: 'sendEmail', title: 'Send Email'},
                    {name: 'archive', title: undefined},
                ],
                clusterElements: [
                    {
                        componentName: 'gmail',
                        description: 'Sends',
                        name: 'sendEmail',
                        title: 'Send Email',
                        type: 'TOOLS',
                    },
                    {componentName: 'gmail', name: 'model', title: 'Model', type: 'MODEL'},
                ],
            })
        );

        const {result} = renderHook(() => useIntegrationComponentElements('gmail', 2));

        expect(hoisted.useGetComponentDefinitionQuery).toHaveBeenCalledWith(
            {componentName: 'gmail', componentVersion: 2},
            true
        );
        expect(result.current.actions).toEqual([
            {description: undefined, id: 'archive', title: 'archive'},
            {description: 'Sends', id: 'sendEmail', title: 'Send Email'},
        ]);
        expect(result.current.tools).toEqual([{description: 'Sends', id: 'GMAIL_SEND_EMAIL', title: 'Send Email'}]);
    });

    it('returns empty lists when the definition has no actions or tools', () => {
        hoisted.useGetComponentDefinitionQuery.mockReturnValue(queryResult({}));

        const {result} = renderHook(() => useIntegrationComponentElements('gmail', 1));

        expect(result.current.actions).toEqual([]);
        expect(result.current.tools).toEqual([]);
    });

    it('waits for the component version before querying', () => {
        hoisted.useGetComponentDefinitionQuery.mockReturnValue(queryResult());

        const {result} = renderHook(() => useIntegrationComponentElements('gmail', undefined));

        expect(hoisted.useGetComponentDefinitionQuery).toHaveBeenCalledWith(
            {componentName: 'gmail', componentVersion: 1},
            false
        );
        expect(result.current.isLoading).toBe(true);
        expect(result.current.actions).toBeUndefined();
        expect(result.current.tools).toBeUndefined();
    });

    it('does not query while the integration card is closed', () => {
        hoisted.useGetComponentDefinitionQuery.mockReturnValue(queryResult());

        const {result} = renderHook(() => useIntegrationComponentElements('gmail', 1, false));

        expect(hoisted.useGetComponentDefinitionQuery).toHaveBeenCalledWith(
            {componentName: 'gmail', componentVersion: 1},
            false
        );
        expect(result.current.isLoading).toBe(false);
    });
});
