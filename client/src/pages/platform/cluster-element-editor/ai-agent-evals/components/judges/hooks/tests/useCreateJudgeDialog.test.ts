import {AiAgentJudgeType} from '@/shared/middleware/graphql';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 7})
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2})
    ),
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    useGetClusterElementDefinitionQuery: vi.fn(() => ({data: undefined})),
    useGetRootComponentClusterElementDefinitions: vi.fn(() => ({data: []})),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: vi.fn(() => ({data: []})),
}));

const {useGetWorkspaceConnectionsQuery} = await import('@/shared/queries/automation/connections.queries');
const mockUseGetWorkspaceConnectionsQuery = vi.mocked(useGetWorkspaceConnectionsQuery);

const {default: useCreateJudgeDialog} = await import('../useCreateJudgeDialog');

describe('useCreateJudgeDialog', () => {
    beforeEach(() => {
        mockUseGetWorkspaceConnectionsQuery.mockClear();
    });

    it('lists the provider connections of the current environment', () => {
        renderHook(() =>
            useCreateJudgeDialog({
                editData: {
                    configuration: {componentName: 'openAi'},
                    id: 'judge-1',
                    name: 'Judge',
                    type: AiAgentJudgeType.LlmRule,
                },
                onClose: vi.fn(),
                onCreate: vi.fn(),
            })
        );

        expect(mockUseGetWorkspaceConnectionsQuery).toHaveBeenCalledWith(
            {componentName: 'openAi', environmentId: 2, id: 7},
            true
        );
    });
});
