import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useAutomationJourney} from './useAutomationJourney';

interface QueryResultI<T> {
    data?: T;
    error: Error | null;
    isLoading: boolean;
}

const hoisted = vi.hoisted(() => ({
    projectDeploymentsResult: {data: undefined, error: null, isLoading: false} as QueryResultI<ProjectDeployment[]>,
    projectsResult: {data: undefined, error: null, isLoading: false} as QueryResultI<Project[]>,
    useGetWorkspaceProjectDeploymentsQuery: vi.fn(),
    useGetWorkspaceProjectsQuery: vi.fn(),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: (...args: unknown[]) => {
        hoisted.useGetWorkspaceProjectsQuery(...args);

        return hoisted.projectsResult;
    },
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: (...args: unknown[]) => {
        hoisted.useGetWorkspaceProjectDeploymentsQuery(...args);

        return hoisted.projectDeploymentsResult;
    },
}));

const publishedProject: Project = {
    id: 4,
    lastPublishedDate: new Date('2026-10-01T10:00:00Z'),
    name: 'Order alerts',
    projectWorkflowIds: [41],
    workspaceId: 5,
};

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.projectDeploymentsResult = {data: [], error: null, isLoading: false};
    hoisted.projectsResult = {data: [publishedProject], error: null, isLoading: false};

    environmentStore.setState({currentEnvironmentId: 2});
    useWorkspaceStore.setState({currentWorkspaceId: 5});
});

describe('useAutomationJourney', () => {
    it('fetches every project with full fields and the deployments of the current environment', () => {
        renderHook(() => useAutomationJourney());

        expect(hoisted.useGetWorkspaceProjectsQuery).toHaveBeenCalledWith({id: 5});
        expect(hoisted.useGetWorkspaceProjectDeploymentsQuery).toHaveBeenCalledWith({environmentId: 2, id: 5});
    });

    it('derives the journey from the fetched data', () => {
        const {result} = renderHook(() => useAutomationJourney());

        expect(result.current.journey?.stage).toBe('deploy');
    });

    it('reports loading while either query is loading', () => {
        hoisted.projectDeploymentsResult = {data: undefined, error: null, isLoading: true};

        const {result} = renderHook(() => useAutomationJourney());

        expect(result.current.isLoading).toBe(true);
        expect(result.current.journey).toBeUndefined();
    });

    it('reports an error when either query fails', () => {
        hoisted.projectsResult = {data: undefined, error: new Error('Request failed'), isLoading: false};

        const {result} = renderHook(() => useAutomationJourney());

        expect(result.current.isError).toBe(true);
        expect(result.current.journey).toBeUndefined();
    });
});
