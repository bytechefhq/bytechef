import {getProjectGitApi} from '@/shared/edition/project-git/projectGitApi';
import {renderHook} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    pullMutation: {isPending: true, mutate: vi.fn()},
    updateMutate: vi.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    updateMutationOptions: undefined as any,
    useGetProjectGitConfigurationQuery: vi.fn(),
    useGetWorkspaceProjectGitConfigurationsQuery: vi.fn(),
    usePullProjectFromGitMutation: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.mutations', () => ({
    usePullProjectFromGitMutation: hoisted.usePullProjectFromGitMutation,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useUpdateProjectGitConfigurationMutation: (options: any) => {
        hoisted.updateMutationOptions = options;

        return {mutate: hoisted.updateMutate};
    },
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.queries', () => ({
    ProjectGitConfigurationKeys: {
        projectGitConfiguration: (id: number) => ['projectGitConfigurations', id],
        projectGitConfigurations: ['projectGitConfigurations'],
    },
    useGetProjectGitConfigurationQuery: hoisted.useGetProjectGitConfigurationQuery,
    useGetWorkspaceProjectGitConfigurationsQuery: hoisted.useGetWorkspaceProjectGitConfigurationsQuery,
}));

await import('../registerEditionModules');

describe('registerEditionModules', () => {
    let queryClient: QueryClient;

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    beforeEach(() => {
        queryClient = new QueryClient();

        hoisted.updateMutate.mockReset();
        hoisted.updateMutationOptions = undefined;
    });

    it('registers the EE project Git queries', () => {
        const projectGitConfiguration = {branch: 'main', enabled: true, projectId: 3};
        const workspaceProjectGitConfigurations = {data: [projectGitConfiguration], error: null, isLoading: false};

        hoisted.useGetProjectGitConfigurationQuery.mockReturnValue({data: projectGitConfiguration});
        hoisted.useGetWorkspaceProjectGitConfigurationsQuery.mockReturnValue(workspaceProjectGitConfigurations);

        const projectGitApi = getProjectGitApi();

        expect(projectGitApi.useProjectGitConfigurationQuery(3)).toEqual({data: projectGitConfiguration});
        expect(hoisted.useGetProjectGitConfigurationQuery).toHaveBeenCalledWith(3);

        expect(projectGitApi.useWorkspaceProjectGitConfigurationsQuery(5, true)).toBe(
            workspaceProjectGitConfigurations
        );
        expect(hoisted.useGetWorkspaceProjectGitConfigurationsQuery).toHaveBeenCalledWith(5, true);
    });

    it('registers the EE pull mutation', () => {
        const onSuccess = vi.fn();

        hoisted.usePullProjectFromGitMutation.mockReturnValue(hoisted.pullMutation);

        expect(getProjectGitApi().usePullProjectFromGitMutation({onSuccess})).toBe(hoisted.pullMutation);
        expect(hoisted.usePullProjectFromGitMutation).toHaveBeenCalledWith({onSuccess});
    });

    it('invalidates the Git configurations and notifies the caller after an update', () => {
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');
        const onSuccess = vi.fn();

        renderHook(() => getProjectGitApi().useUpdateProjectGitConfigurationMutation({onSuccess}), {wrapper});

        hoisted.updateMutationOptions.onSuccess(undefined, {id: 3});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projectGitConfigurations', 3]});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projectGitConfigurations']});
        expect(onSuccess).toHaveBeenCalled();
    });

    it('passes the update input and per-call callbacks through to the EE mutation', () => {
        const mutateCallbacks = {onSuccess: vi.fn()};
        const input = {id: 3, projectGitConfiguration: {branch: 'main', enabled: true}};

        const {result} = renderHook(() => getProjectGitApi().useUpdateProjectGitConfigurationMutation({}), {
            wrapper,
        });

        result.current.mutate(input, mutateCallbacks);

        expect(hoisted.updateMutate).toHaveBeenCalledWith(input, mutateCallbacks);
    });
});
