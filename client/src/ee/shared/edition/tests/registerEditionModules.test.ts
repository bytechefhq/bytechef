import {getProjectGitApi} from '@/shared/edition/project-git/projectGitApi';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    invalidateQueriesMock,
    mutateMock,
    updateMutationOptions,
    useGetProjectGitConfigurationQueryMock,
    useGetWorkspaceProjectGitConfigurationsQueryMock,
    usePullProjectFromGitMutationMock,
} = vi.hoisted(() => ({
    invalidateQueriesMock: vi.fn(),
    mutateMock: vi.fn(),
    updateMutationOptions: {
        current: undefined as {onSuccess?: (result: unknown, variables: {id: number}) => void} | undefined,
    },
    useGetProjectGitConfigurationQueryMock: vi.fn(),
    useGetWorkspaceProjectGitConfigurationsQueryMock: vi.fn(),
    usePullProjectFromGitMutationMock: vi.fn(),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.mutations', () => ({
    usePullProjectFromGitMutation: usePullProjectFromGitMutationMock,
    useUpdateProjectGitConfigurationMutation: (options: typeof updateMutationOptions.current) => {
        updateMutationOptions.current = options;

        return {mutate: mutateMock};
    },
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.queries', () => ({
    ProjectGitConfigurationKeys: {
        projectGitConfiguration: (id: number) => ['projectGitConfiguration', id],
        projectGitConfigurations: ['projectGitConfigurations'],
    },
    useGetProjectGitConfigurationQuery: useGetProjectGitConfigurationQueryMock,
    useGetWorkspaceProjectGitConfigurationsQuery: useGetWorkspaceProjectGitConfigurationsQueryMock,
}));

import '../registerEditionModules';

describe('registerEditionModules', () => {
    beforeEach(() => {
        invalidateQueriesMock.mockReset();
        invalidateQueriesMock.mockResolvedValue(undefined);
        mutateMock.mockReset();
        updateMutationOptions.current = undefined;
    });

    it('registers the EE project git queries and pull mutation', () => {
        const configuration = {branch: 'main', enabled: true, projectId: 7};
        const pullMutation = {isPending: false, mutate: vi.fn()};
        const onSuccess = vi.fn();

        useGetProjectGitConfigurationQueryMock.mockReturnValue({data: configuration});
        useGetWorkspaceProjectGitConfigurationsQueryMock.mockReturnValue({
            data: [configuration],
            error: null,
            isLoading: false,
        });
        usePullProjectFromGitMutationMock.mockReturnValue(pullMutation);

        const projectGitApi = getProjectGitApi();

        expect(projectGitApi.useProjectGitConfigurationQuery(7)).toEqual({data: configuration});
        expect(useGetProjectGitConfigurationQueryMock).toHaveBeenCalledWith(7);

        expect(projectGitApi.useWorkspaceProjectGitConfigurationsQuery(3, true).data).toEqual([configuration]);
        expect(useGetWorkspaceProjectGitConfigurationsQueryMock).toHaveBeenCalledWith(3, true);

        expect(projectGitApi.usePullProjectFromGitMutation({onSuccess})).toBe(pullMutation);
        expect(usePullProjectFromGitMutationMock).toHaveBeenCalledWith({onSuccess});
    });

    it('invalidates both git configuration queries and notifies the caller after an update', () => {
        const onSuccess = vi.fn();

        const {result} = renderHook(() => getProjectGitApi().useUpdateProjectGitConfigurationMutation({onSuccess}));

        updateMutationOptions.current?.onSuccess?.(undefined, {id: 7});

        expect(invalidateQueriesMock).toHaveBeenCalledWith({queryKey: ['projectGitConfiguration', 7]});
        expect(invalidateQueriesMock).toHaveBeenCalledWith({queryKey: ['projectGitConfigurations']});
        expect(onSuccess).toHaveBeenCalledTimes(1);

        const input = {id: 7, projectGitConfiguration: {branch: 'main', enabled: true}};
        const mutateCallbacks = {onSuccess: vi.fn()};

        result.current.mutate(input, mutateCallbacks);

        expect(mutateMock).toHaveBeenCalledWith(input, mutateCallbacks);
    });
});
