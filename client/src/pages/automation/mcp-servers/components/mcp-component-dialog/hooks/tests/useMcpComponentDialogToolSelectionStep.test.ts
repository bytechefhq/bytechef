import {McpComponent, McpToolsByComponentIdQuery} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialogToolSelectionStep from '../useMcpComponentDialogToolSelectionStep';

const hoisted = vi.hoisted(() => ({
    useGetComponentDefinitionQuery: vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: () => ({data: [], isLoading: false, refetch: vi.fn()}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: hoisted.useGetComponentDefinitionQuery,
}));

const clusterElements = [
    {
        componentName: 'hubspot',
        componentVersion: 1,
        description: 'Create a contact',
        name: 'createContact',
        title: 'Create Contact',
        type: 'TOOLS',
    },
];

describe('useMcpComponentDialogToolSelectionStep', () => {
    beforeEach(() => {
        hoisted.useGetComponentDefinitionQuery.mockReset();
        hoisted.useGetComponentDefinitionQuery.mockReturnValue({data: {clusterElements}, isLoading: false});
    });

    it('pre-selects the saved tools that the component still offers', () => {
        const onToolsChange = vi.fn();

        renderHook(() =>
            useMcpComponentDialogToolSelectionStep({
                existingTools: {
                    mcpToolsByComponentId: [
                        {name: 'createContact', parameters: {firstName: "=fromAi('firstName', 'STRING')"}},
                    ],
                } as unknown as McpToolsByComponentIdQuery,
                mcpComponent: undefined as unknown as McpComponent,
                onConnectionChange: vi.fn(),
                onToolsChange,
                open: true,
                selectedComponent: {name: 'hubspot', version: 1} as ComponentDefinitionBasic,
                selectedTools: [],
            })
        );

        expect(onToolsChange).toHaveBeenCalledWith([
            {
                componentName: 'hubspot',
                componentVersion: 1,
                description: 'Create a contact',
                name: 'createContact',
                title: 'Create Contact',
            },
        ]);
    });
});
