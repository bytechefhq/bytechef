import {McpComponent} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialogComponentSelectionStep from '../useMcpComponentDialogComponentSelectionStep';

const hoisted = vi.hoisted(() => ({
    useEmbeddedMcpComponentsByServerIdQuery: vi.fn(),
    useMcpComponentDefinitionsQuery: vi.fn(),
    useMcpComponentIntegrationsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEmbeddedMcpComponentsByServerIdQuery: hoisted.useEmbeddedMcpComponentsByServerIdQuery,
    useMcpComponentDefinitionsQuery: hoisted.useMcpComponentDefinitionsQuery,
    useMcpComponentIntegrationsQuery: hoisted.useMcpComponentIntegrationsQuery,
}));

describe('useMcpComponentDialogComponentSelectionStep', () => {
    beforeEach(() => {
        hoisted.useEmbeddedMcpComponentsByServerIdQuery.mockReturnValue({
            data: {embeddedMcpComponentsByServerId: []},
            isLoading: false,
        });
        hoisted.useMcpComponentDefinitionsQuery.mockReturnValue({
            data: {
                mcpComponentDefinitions: [
                    {description: 'Gmail component', icon: 'gmail.svg', name: 'googleMail', title: 'Gmail', version: 1},
                    {description: 'Slack component', icon: 'slack.svg', name: 'slack', title: 'Slack', version: 2},
                ],
            },
            isLoading: false,
        });
        hoisted.useMcpComponentIntegrationsQuery.mockReturnValue({
            data: {
                mcpComponentIntegrations: [
                    {componentName: 'googleMail', id: '5', name: 'Gmail'},
                    {componentName: 'googleMail', id: '6', name: 'Gmail Code'},
                    {componentName: 'slack', id: '7', name: 'Slack'},
                    {componentName: 'unknown', id: '8', name: 'Unknown'},
                ],
            },
            isLoading: false,
        });
    });

    it('offers one option per integration with its component definition', () => {
        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true, '1'));

        expect(result.current.filteredComponents).toEqual([
            {
                description: 'Gmail component',
                icon: 'gmail.svg',
                id: '5',
                integrationId: '5',
                name: 'googleMail',
                title: 'Gmail',
                version: 1,
            },
            {
                description: 'Gmail component',
                icon: 'gmail.svg',
                id: '6',
                integrationId: '6',
                name: 'googleMail',
                title: 'Gmail Code',
                version: 1,
            },
            {
                description: 'Slack component',
                icon: 'slack.svg',
                id: '7',
                integrationId: '7',
                name: 'slack',
                title: 'Slack',
                version: 2,
            },
        ]);
    });

    it('offers only the integrations of the given component', () => {
        const {result} = renderHook(() =>
            useMcpComponentDialogComponentSelectionStep(true, '1', {
                componentName: 'googleMail',
                componentVersion: 1,
                id: '9',
            } as McpComponent)
        );

        expect(result.current.filteredComponents.map((option) => option.integrationId)).toEqual(['5', '6']);
    });

    it('filters the options by integration name', () => {
        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true, '1'));

        act(() => result.current.setSearchTerm('code'));

        expect(result.current.filteredComponents.map((option) => option.integrationId)).toEqual(['6']);
    });

    it('loads the options only while the dialog is open', () => {
        renderHook(() => useMcpComponentDialogComponentSelectionStep(false, '1'));

        expect(hoisted.useMcpComponentDefinitionsQuery).toHaveBeenLastCalledWith({}, {enabled: false});
        expect(hoisted.useMcpComponentIntegrationsQuery).toHaveBeenLastCalledWith({}, {enabled: false});
        expect(hoisted.useEmbeddedMcpComponentsByServerIdQuery).toHaveBeenLastCalledWith(
            {mcpServerId: '1'},
            {enabled: false}
        );
    });

    it('marks every integration of a component version that is already on the server as added', () => {
        hoisted.useEmbeddedMcpComponentsByServerIdQuery.mockReturnValue({
            data: {embeddedMcpComponentsByServerId: [{componentName: 'googleMail', componentVersion: 1, id: '9'}]},
            isLoading: false,
        });

        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true, '1'));

        expect(
            result.current.filteredComponents
                .filter((option) => result.current.isComponentAdded(option))
                .map((option) => option.integrationId)
        ).toEqual(['5', '6']);
    });

    it('matches a component definition without a version against version 1 on the server', () => {
        hoisted.useMcpComponentDefinitionsQuery.mockReturnValue({
            data: {mcpComponentDefinitions: [{name: 'slack', title: 'Slack', version: null}]},
            isLoading: false,
        });
        hoisted.useEmbeddedMcpComponentsByServerIdQuery.mockReturnValue({
            data: {embeddedMcpComponentsByServerId: [{componentName: 'slack', componentVersion: 1, id: '9'}]},
            isLoading: false,
        });

        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true, '1'));

        expect(result.current.filteredComponents.map((option) => result.current.isComponentAdded(option))).toEqual([
            true,
        ]);
    });

    it('does not load the server components without a server id', () => {
        renderHook(() => useMcpComponentDialogComponentSelectionStep(true, ''));

        expect(hoisted.useEmbeddedMcpComponentsByServerIdQuery).toHaveBeenLastCalledWith(
            {mcpServerId: ''},
            {enabled: false}
        );
    });

    it('stays loading until the server components are loaded', () => {
        hoisted.useEmbeddedMcpComponentsByServerIdQuery.mockReturnValue({data: undefined, isLoading: true});

        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true, '1'));

        expect(result.current.isLoadingComponents).toBe(true);
    });

    it('does not mark the component being edited as added', () => {
        const mcpComponent = {componentName: 'googleMail', componentVersion: 1, id: '9'} as McpComponent;

        hoisted.useEmbeddedMcpComponentsByServerIdQuery.mockReturnValue({
            data: {embeddedMcpComponentsByServerId: [mcpComponent]},
            isLoading: false,
        });

        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true, '1', mcpComponent));

        expect(result.current.filteredComponents.some((option) => result.current.isComponentAdded(option))).toBe(false);
    });
});
