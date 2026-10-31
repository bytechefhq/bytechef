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
        expect(result.current.integrationUnavailable).toBe(false);
        expect(hoisted.useMcpComponentIntegrationsQuery).toHaveBeenLastCalledWith({}, {enabled: true});
    });

    it('does not load the integrations for a component without an integration', () => {
        const {result} = renderHook(() => useMcpComponentListItem('googleMail', 1, null));

        expect(hoisted.useMcpComponentIntegrationsQuery).toHaveBeenLastCalledWith({}, {enabled: false});
        expect(result.current.integrationName).toBeUndefined();
    });

    it('reports the bound integration as unavailable once the offered integrations do not include it', () => {
        const {result} = renderHook(() => useMcpComponentListItem('googleMail', 1, '99'));

        expect(result.current.integrationName).toBeUndefined();
        expect(result.current.integrationUnavailable).toBe(true);
    });

    it('does not report the bound integration as unavailable while the integrations load', () => {
        hoisted.useMcpComponentIntegrationsQuery.mockReturnValue({data: undefined});

        const {result} = renderHook(() => useMcpComponentListItem('googleMail', 1, '99'));

        expect(result.current.integrationUnavailable).toBe(false);
    });

    it('does not report a component without an integration as unavailable', () => {
        const {result} = renderHook(() => useMcpComponentListItem('googleMail', 1, null));

        expect(result.current.integrationUnavailable).toBe(false);
    });
});
