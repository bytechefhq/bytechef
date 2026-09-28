import {DEVELOPMENT_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useOpenInProject from '../useOpenInProject';

const {fetchQueryMock, navigateMock, toastMock} = vi.hoisted(() => ({
    fetchQueryMock: vi.fn(),
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

describe('useOpenInProject', () => {
    beforeEach(() => {
        fetchQueryMock.mockReset();
        navigateMock.mockReset();
        toastMock.mockReset();

        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
    });

    it('allows opening the project editor only in Development', () => {
        const {rerender, result} = renderHook(() => useOpenInProject());

        expect(result.current.canOpenInProject).toBe(true);

        act(() => {
            environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});
        });

        rerender();

        expect(result.current.canOpenInProject).toBe(false);
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
