import {renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useMcpComponentList from '../useMcpComponentList';

const hoisted = vi.hoisted(() => ({
    useEmbeddedMcpComponentsByServerIdQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEmbeddedMcpComponentsByServerIdQuery: hoisted.useEmbeddedMcpComponentsByServerIdQuery,
}));

describe('useMcpComponentList', () => {
    it('reads the components through the embedded query, including each tool enabled flag', () => {
        const data = {
            embeddedMcpComponentsByServerId: [
                {
                    componentName: 'affinity',
                    id: '1',
                    mcpTools: [{enabled: true, id: '42', name: 'createOpportunity'}],
                },
            ],
        };

        hoisted.useEmbeddedMcpComponentsByServerIdQuery.mockReturnValue({data, isLoading: false});

        const {result} = renderHook(() => useMcpComponentList('7'));

        expect(hoisted.useEmbeddedMcpComponentsByServerIdQuery).toHaveBeenCalledWith({mcpServerId: '7'});
        expect(result.current.data?.embeddedMcpComponentsByServerId?.[0]?.mcpTools?.[0]?.enabled).toBe(true);
        expect(result.current.isMcpComponentsLoading).toBe(false);
    });
});
