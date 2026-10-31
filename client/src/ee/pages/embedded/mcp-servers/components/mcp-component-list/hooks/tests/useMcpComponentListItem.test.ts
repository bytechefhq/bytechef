import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentListItem from '../useMcpComponentListItem';

const hoisted = vi.hoisted(() => ({
    useGetComponentDefinitionQuery: vi.fn(),
    useMcpComponentIntegrationsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpComponentIntegrationsQuery: hoisted.useMcpComponentIntegrationsQuery,
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: hoisted.useGetComponentDefinitionQuery,
}));

describe('useMcpComponentListItem', () => {
    beforeEach(() => {
        hoisted.useGetComponentDefinitionQuery.mockReturnValue({data: undefined});
        hoisted.useMcpComponentIntegrationsQuery.mockReturnValue({
            data: {
                mcpComponentIntegrations: [
                    {componentName: 'googleMail', id: '5', name: 'Gmail'},
                    {componentName: 'googleMail', id: '6', name: 'Gmail Code'},
                ],
            },
        });
    });

    it('names the integration the component is bound to', () => {
        const {result} = renderHook(() => useMcpComponentListItem('googleMail', 1, '6'));

        expect(result.current.integrationName).toBe('Gmail Code');
        expect(hoisted.useMcpComponentIntegrationsQuery).toHaveBeenLastCalledWith({}, {enabled: true});
    });

    it('does not load the integrations for a component without an integration', () => {
        const {result} = renderHook(() => useMcpComponentListItem('googleMail', 1, null));

        expect(hoisted.useMcpComponentIntegrationsQuery).toHaveBeenLastCalledWith({}, {enabled: false});
        expect(result.current.integrationName).toBeUndefined();
    });
});
