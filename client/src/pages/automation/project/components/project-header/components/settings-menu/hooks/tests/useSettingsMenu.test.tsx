import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useSettingsMenu} from '../useSettingsMenu';

const hoisted = vi.hoisted(() => ({
    clearWorkflowHistory: vi.fn(),
    deleteProjectVersionMutate: vi.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    deleteProjectVersionOptions: undefined as any,
    navigate: vi.fn(),
    noopMutation: () => ({isPending: false, mutate: vi.fn()}),
    restoreProjectVersionMutate: vi.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    restoreProjectVersionOptions: undefined as any,
    toast: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    clearWorkflowHistory: hoisted.clearWorkflowHistory,
}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    useDeleteProjectMutation: hoisted.noopMutation,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useDeleteProjectVersionMutation: (options: any) => {
        hoisted.deleteProjectVersionOptions = options;

        return {isPending: false, mutate: hoisted.deleteProjectVersionMutate};
    },
    useDuplicateProjectMutation: hoisted.noopMutation,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useRestoreProjectVersionMutation: (options: any) => {
        hoisted.restoreProjectVersionOptions = options;

        return {isPending: false, mutate: hoisted.restoreProjectVersionMutate};
    },
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useDeleteWorkflowMutation: hoisted.noopMutation,
    useDuplicateWorkflowMutation: hoisted.noopMutation,
}));

vi.mock('@/shared/queries/automation/projectVersions.queries', () => ({
    ProjectVersionKeys: {projectProjectVersions: (projectId: number) => ['projectVersions', 'projects', projectId]},
    useGetProjectVersionsQuery: () => ({data: []}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [new URLSearchParams('environmentId=1')],
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

const project: Project = {id: 7, name: 'Project', projectWorkflowIds: [11, 12], workspaceId: 1};
const workflow: Workflow = {id: 'workflow-1', projectWorkflowId: 11};

describe('useSettingsMenu', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();

        hoisted.clearWorkflowHistory.mockReset();
        hoisted.deleteProjectVersionMutate.mockReset();
        hoisted.navigate.mockReset();
        hoisted.restoreProjectVersionMutate.mockReset();
        hoisted.toast.mockReset();
    });

    const renderSettingsMenu = () =>
        renderHook(() => useSettingsMenu({project, workflow}), {
            wrapper: ({children}: {children: ReactNode}) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

    it('deletes and restores the given version of the project', () => {
        const {result} = renderSettingsMenu();
        const onSuccess = vi.fn();

        result.current.handleDeleteProjectVersion(2, onSuccess);
        result.current.handleRestoreProjectVersion(3, onSuccess);

        expect(hoisted.deleteProjectVersionMutate).toHaveBeenCalledWith({id: 7, projectVersion: 2}, {onSuccess});
        expect(hoisted.restoreProjectVersionMutate).toHaveBeenCalledWith({id: 7, projectVersion: 3}, {onSuccess});
    });

    it('refreshes the version list after a version is deleted', () => {
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderSettingsMenu();

        hoisted.deleteProjectVersionOptions.onSuccess(undefined, {id: 7, projectVersion: 2});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projectVersions', 'projects', 7]});
        expect(hoisted.toast).toHaveBeenCalledWith('Version V2 deleted.');
    });

    it('stays on the current workflow when the restored version still contains it', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...project, projectWorkflowIds: [11, 13]});

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreProjectVersionOptions.onSuccess(undefined, {id: 7, projectVersion: 1});
        });

        expect(hoisted.navigate).not.toHaveBeenCalled();
        expect(hoisted.clearWorkflowHistory).toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projectVersions', 'projects', 7]});
        expect(hoisted.toast).toHaveBeenCalledWith('Version V1 applied to the draft.');
    });

    it('closes the node details panel after a version is restored', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...project, projectWorkflowIds: [11, 13]});

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'dataStorage_1'} as NodeDataType,
            workflowNodeDetailsPanelOpen: true,
        });

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreProjectVersionOptions.onSuccess(undefined, {id: 7, projectVersion: 1});
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
    });

    it('opens the first restored workflow when the restored version no longer contains the current one', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...project, projectWorkflowIds: [13, 14]});

        const removeQueriesSpy = vi.spyOn(queryClient, 'removeQueries');

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreProjectVersionOptions.onSuccess(undefined, {id: 7, projectVersion: 1});
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects/7/project-workflows/13?environmentId=1');
        expect(removeQueriesSpy).toHaveBeenCalled();
    });

    it('returns to the project list when the restored version has no workflows', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...project, projectWorkflowIds: []});

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreProjectVersionOptions.onSuccess(undefined, {id: 7, projectVersion: 1});
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects');
    });

    it('makes no Git requests when no edition registers the project Git API', () => {
        const fetchSpy = vi.spyOn(globalThis, 'fetch');
        const onSuccess = vi.fn();

        const {result} = renderSettingsMenu();

        result.current.handlePullProjectFromGitClick();
        result.current.handleUpdateProjectGitConfigurationSubmit({
            onSuccess,
            projectGitConfiguration: {branch: 'main', enabled: true},
        });

        expect(result.current.projectGitConfiguration).toBeUndefined();
        expect(fetchSpy).not.toHaveBeenCalled();
        expect(onSuccess).not.toHaveBeenCalled();

        fetchSpy.mockRestore();
    });
});
