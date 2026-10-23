import {describe, expect, it, vi} from 'vitest';

import {ProjectGitApiI, getProjectGitApi, registerProjectGitApi} from '../projectGitApi';

describe('projectGitApi', () => {
    it('returns empty data and ignores mutations until an edition registers an implementation', () => {
        const projectGitApi = getProjectGitApi();

        expect(projectGitApi.useProjectGitConfigurationQuery(1)).toEqual({data: undefined});
        expect(projectGitApi.useWorkspaceProjectGitConfigurationsQuery(1, true)).toEqual({
            data: undefined,
            error: null,
            isLoading: false,
        });

        const pullMutation = projectGitApi.usePullProjectFromGitMutation({});

        expect(pullMutation.isPending).toBe(false);
        expect(() => pullMutation.mutate({id: 1})).not.toThrow();
        expect(() =>
            projectGitApi
                .useUpdateProjectGitConfigurationMutation({})
                .mutate({id: 1, projectGitConfiguration: {branch: 'main', enabled: true}})
        ).not.toThrow();
    });

    it('returns the registered implementation', () => {
        const registeredProjectGitApi: ProjectGitApiI = {
            useProjectGitConfigurationQuery: vi.fn(),
            usePullProjectFromGitMutation: vi.fn(),
            useUpdateProjectGitConfigurationMutation: vi.fn(),
            useWorkspaceProjectGitConfigurationsQuery: vi.fn(),
        };

        registerProjectGitApi(registeredProjectGitApi);

        expect(getProjectGitApi()).toBe(registeredProjectGitApi);
    });
});
