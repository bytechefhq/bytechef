import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialogComponentSelectionStep from '../useMcpComponentDialogComponentSelectionStep';

const hoisted = vi.hoisted(() => ({
    useMcpComponentDefinitionsQuery: vi.fn(),
    useMcpComponentIntegrationsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpComponentDefinitionsQuery: hoisted.useMcpComponentDefinitionsQuery,
    useMcpComponentIntegrationsQuery: hoisted.useMcpComponentIntegrationsQuery,
}));

describe('useMcpComponentDialogComponentSelectionStep', () => {
    beforeEach(() => {
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
        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true));

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
        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true, 'googleMail'));

        expect(result.current.filteredComponents.map((option) => option.integrationId)).toEqual(['5', '6']);
    });

    it('filters the options by integration name', () => {
        const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true));

        act(() => result.current.setSearchTerm('code'));

        expect(result.current.filteredComponents.map((option) => option.integrationId)).toEqual(['6']);
    });

    it('loads the options only while the dialog is open', () => {
        renderHook(() => useMcpComponentDialogComponentSelectionStep(false));

        expect(hoisted.useMcpComponentDefinitionsQuery).toHaveBeenLastCalledWith({}, {enabled: false});
        expect(hoisted.useMcpComponentIntegrationsQuery).toHaveBeenLastCalledWith({}, {enabled: false});
    });
});
