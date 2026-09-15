import {loadProjectWorkflowEditor} from '@/routes';
import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('loadProjectWorkflowEditor', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();
    });

    afterEach(() => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});

        vi.restoreAllMocks();
    });

    it('loads the project when Development is selected', async () => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});

        const project = {id: 7, name: 'Project'};

        const ensureQueryDataSpy = vi.spyOn(queryClient, 'ensureQueryData').mockResolvedValue(project);

        const result = await loadProjectWorkflowEditor(queryClient, 7);

        expect(ensureQueryDataSpy).toHaveBeenCalledTimes(1);
        expect(result).toBe(project);
    });

    it('redirects to deployments instead of opening the editor outside Development', async () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        const ensureQueryDataSpy = vi.spyOn(queryClient, 'ensureQueryData');

        const result = await loadProjectWorkflowEditor(queryClient, 7);

        expect(ensureQueryDataSpy).not.toHaveBeenCalled();
        expect(result).toBeInstanceOf(Response);
        expect((result as Response).headers.get('Location')).toBe('/automation/deployments');
    });
});
