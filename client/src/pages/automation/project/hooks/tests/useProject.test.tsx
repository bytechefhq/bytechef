import {Workflow} from '@/shared/middleware/automation/configuration';
import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useProject} from '../useProject';

const PROJECT_ID = 7;

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
    useParams: () => ({projectId: String(PROJECT_ID), projectWorkflowId: '3'}),
    useSearchParams: () => [new URLSearchParams(''), vi.fn()],
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectWorkflows.queries')>()),
    useGetProjectWorkflowQuery: () => ({data: undefined, isLoading: false}),
    useGetProjectWorkflowsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/projectCategories.queries', () => ({
    useGetProjectCategoriesQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/projectTags.queries', () => ({
    useGetProjectTagsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/mutations/platform/workflowNodeParameters.mutations', () => ({
    useDeleteClusterElementParameterMutation: vi.fn(() => ({})),
    useDeleteWorkflowNodeParameterMutation: vi.fn(() => ({})),
    useUpdateClusterElementParameterMutation: vi.fn(() => ({})),
    useUpdateWorkflowNodeParameterMutation: vi.fn(() => ({})),
}));

vi.mock('@/shared/mutations/platform/workflows.mutations', () => ({
    default: vi.fn(() => ({})),
}));

const PARAMETER_MUTATION_VARIABLES = {
    environmentId: 1,
    id: 'workflow-b',
    workflowNodeName: 'logger_1',
};

interface ParameterMutationOptionsI {
    onSuccess?: (result: {version?: number}, variables: typeof PARAMETER_MUTATION_VARIABLES) => void;
}

type ParameterMutationHookType = (options?: ParameterMutationOptionsI) => unknown;

const parameterMutationHooks: [string, ParameterMutationHookType][] = [
    ['useDeleteWorkflowNodeParameterMutation', useDeleteWorkflowNodeParameterMutation],
    ['useDeleteClusterElementParameterMutation', useDeleteClusterElementParameterMutation],
    ['useUpdateWorkflowNodeParameterMutation', useUpdateWorkflowNodeParameterMutation],
    ['useUpdateClusterElementParameterMutation', useUpdateClusterElementParameterMutation],
];

function renderUseProjectWithCachedWorkflows() {
    const queryClient = new QueryClient();

    queryClient.setQueryData(ProjectWorkflowKeys.projectWorkflows(PROJECT_ID), [
        {id: 'workflow-a', version: 1},
        {id: 'workflow-b', version: 1},
    ]);

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    renderHook(() => useProject(), {wrapper});

    return queryClient;
}

function getCachedProjectWorkflows(queryClient: QueryClient) {
    return queryClient.getQueryData<Workflow[]>(ProjectWorkflowKeys.projectWorkflows(PROJECT_ID));
}

describe('useProject', () => {
    afterEach(() => {
        vi.clearAllMocks();
    });

    it.each(parameterMutationHooks)(
        'syncs the cached project workflow version after %s succeeds',
        (_name, useParameterMutation) => {
            const queryClient = renderUseProjectWithCachedWorkflows();

            const mutationOptions = vi.mocked(useParameterMutation).mock.calls[0][0];

            act(() => {
                mutationOptions?.onSuccess?.({version: 4}, PARAMETER_MUTATION_VARIABLES);
            });

            expect(getCachedProjectWorkflows(queryClient)).toEqual([
                {id: 'workflow-a', version: 1},
                {id: 'workflow-b', version: 4},
            ]);
        }
    );

    it('syncs the cached project workflow version after the workflow editor save succeeds', () => {
        const queryClient = renderUseProjectWithCachedWorkflows();

        const editorMutationOptions = vi.mocked(useUpdatePlatformWorkflowMutation).mock.calls[0][0];

        act(() => {
            editorMutationOptions.onSuccess?.({id: 'workflow-b', version: 4} as Workflow);
        });

        expect(getCachedProjectWorkflows(queryClient)).toEqual([
            {id: 'workflow-a', version: 1},
            {id: 'workflow-b', version: 4},
        ]);
    });
});
