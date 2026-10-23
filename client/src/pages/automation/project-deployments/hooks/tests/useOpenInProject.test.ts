import {DEVELOPMENT_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useOpenInProject from '../useOpenInProject';

const {fetchQueryMock, grantedScopesByEnvironment, navigateMock, toastMock} = vi.hoisted(() => ({
    fetchQueryMock: vi.fn(),
    grantedScopesByEnvironment: {} as Record<number, string[]>,
    navigateMock: vi.fn(),
    toastMock: vi.fn(),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({fetchQuery: fetchQueryMock}),
    };
});

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();

    return {
        ...actual,
        useNavigate: () => navigateMock,
    };
});

vi.mock('sonner', () => ({toast: toastMock}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', async () => {
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string, environmentId?: number) =>
            (
                grantedScopesByEnvironment[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ??
                []
            ).includes(scope),
    };
});

describe('useOpenInProject', () => {
    beforeEach(() => {
        fetchQueryMock.mockReset();
        navigateMock.mockReset();
        toastMock.mockReset();

        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});

        grantedScopesByEnvironment[DEVELOPMENT_ENVIRONMENT] = ['WORKFLOW_VIEW'];
        grantedScopesByEnvironment[STAGING_ENVIRONMENT] = ['WORKFLOW_VIEW'];
    });

    it('allows opening in the project with WORKFLOW_VIEW in the current environment and in Development', () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        const {result} = renderHook(() => useOpenInProject());

        expect(result.current.canOpenInProject).toBe(true);
    });

    it('refuses opening in the project without WORKFLOW_VIEW in Development', () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        grantedScopesByEnvironment[DEVELOPMENT_ENVIRONMENT] = [];

        const {result} = renderHook(() => useOpenInProject());

        expect(result.current.canOpenInProject).toBe(false);
    });

    it('refuses opening in the project without WORKFLOW_VIEW in the current environment', () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        grantedScopesByEnvironment[STAGING_ENVIRONMENT] = ['DEPLOYMENT_VIEW'];

        const {result} = renderHook(() => useOpenInProject());

        expect(result.current.canOpenInProject).toBe(false);
    });

    it('switches to Development before opening the project from another environment', async () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        fetchQueryMock.mockResolvedValue({id: 7, projectWorkflowIds: [11]});

        navigateMock.mockImplementation(() => {
            expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
        });

        const {result} = renderHook(() => useOpenInProject());

        await act(() => result.current.openProject(7));

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/7/project-workflows/11');
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
    });

    it('keeps the environment when the lookup fails outside Development', async () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        fetchQueryMock.mockRejectedValue(new Error('network'));

        const {result} = renderHook(() => useOpenInProject());

        await act(() => result.current.openProjectWorkflow(7, 'deployed-uuid'));

        expect(environmentStore.getState().currentEnvironmentId).toBe(STAGING_ENVIRONMENT);
    });

    it('opens the first workflow of the project', async () => {
        const onBeforeNavigate = vi.fn();

        fetchQueryMock.mockResolvedValue({id: 7, projectWorkflowIds: [11, 12]});

        const {result} = renderHook(() => useOpenInProject({onBeforeNavigate}));

        await act(() => result.current.openProject(7));

        expect(onBeforeNavigate).toHaveBeenCalledTimes(1);
        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/7/project-workflows/11');
    });

    it('stays put with a toast when the project has no workflows', async () => {
        const onBeforeNavigate = vi.fn();

        fetchQueryMock.mockResolvedValue({id: 7, projectWorkflowIds: []});

        const {result} = renderHook(() => useOpenInProject({onBeforeNavigate}));

        await act(() => result.current.openProject(7));

        expect(toastMock).toHaveBeenCalledWith('The project has no workflows to open.');
        expect(onBeforeNavigate).not.toHaveBeenCalled();
        expect(navigateMock).not.toHaveBeenCalled();
    });

    it("opens the latest version's workflow matching the deployed workflow's uuid, not the deployed row", async () => {
        fetchQueryMock.mockResolvedValue([
            {projectWorkflowId: 21, workflowUuid: 'other-uuid'},
            {projectWorkflowId: 22, workflowUuid: 'deployed-uuid'},
        ]);

        const {result} = renderHook(() => useOpenInProject());

        await act(() => result.current.openProjectWorkflow(7, 'deployed-uuid'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/7/project-workflows/22');
    });

    it('stays put with a toast when the workflow is gone from the latest project version', async () => {
        const onBeforeNavigate = vi.fn();

        fetchQueryMock.mockResolvedValue([{projectWorkflowId: 21, workflowUuid: 'other-uuid'}]);

        const {result} = renderHook(() => useOpenInProject({onBeforeNavigate}));

        await act(() => result.current.openProjectWorkflow(7, 'deployed-uuid'));

        expect(toastMock).toHaveBeenCalledWith('This workflow no longer exists in the latest project version.');
        expect(onBeforeNavigate).not.toHaveBeenCalled();
        expect(navigateMock).not.toHaveBeenCalled();
    });

    it('does not navigate when the lookup fails', async () => {
        fetchQueryMock.mockRejectedValue(new Error('network'));

        const {result} = renderHook(() => useOpenInProject());

        await act(() => result.current.openProjectWorkflow(7, 'deployed-uuid'));

        expect(navigateMock).not.toHaveBeenCalled();
    });
});
