import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useCreateProjectWorkflow} from '../useCreateProjectWorkflow';

const hoisted = vi.hoisted(() => ({
    captureProjectWorkflowCreated: vi.fn(),
    mutationOptions: {current: undefined as {onSuccess?: (response: {projectWorkflowId: number}) => void} | undefined},
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectWorkflowCreated: hoisted.captureProjectWorkflowCreated}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useCreateProjectWorkflowMutation: (options: typeof hoisted.mutationOptions.current) => {
        hoisted.mutationOptions.current = options;

        return {mutate: vi.fn()};
    },
}));

const renderUseCreateProjectWorkflow = (
    bottomResizablePanelRef?: {current: Pick<PanelImperativeHandle, 'resize'> | null},
    projectId = 5,
    queryClient = new QueryClient()
) => {
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    renderHook(
        () =>
            useCreateProjectWorkflow({
                bottomResizablePanelRef: bottomResizablePanelRef as {current: PanelImperativeHandle | null},
                projectId,
            }),
        {wrapper}
    );

    return {invalidateQueriesSpy};
};

describe('useCreateProjectWorkflow', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.mutationOptions.current = undefined;

        useWorkspaceStore.setState({currentWorkspaceId: 10});
        useWorkflowEditorStore.setState({showBottomPanel: true});
    });

    it('refreshes the project workflow lists and opens the created workflow', () => {
        const resize = vi.fn();
        const {invalidateQueriesSpy} = renderUseCreateProjectWorkflow({current: {resize}});

        hoisted.mutationOptions.current?.onSuccess?.({projectWorkflowId: 42});

        expect(hoisted.captureProjectWorkflowCreated).toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectWorkflowKeys.projectWorkflows(5)});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectWorkflowKeys.workflows});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectKeys.filteredProjects({id: 10})});
        expect(useWorkflowEditorStore.getState().showBottomPanel).toBe(false);
        expect(resize).toHaveBeenCalledWith(0);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects/5/project-workflows/42');
    });

    it('marks the cached project stale so Publish sees the added workflow', () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(ProjectKeys.project(5), {id: 5, unpublishedChanges: false});

        renderUseCreateProjectWorkflow(undefined, 5, queryClient);

        hoisted.mutationOptions.current?.onSuccess?.({projectWorkflowId: 42});

        expect(queryClient.getQueryState(ProjectKeys.project(5))?.isInvalidated).toBe(true);
    });

    it('opens the created workflow without a bottom panel to collapse', () => {
        renderUseCreateProjectWorkflow(undefined, 8);

        hoisted.mutationOptions.current?.onSuccess?.({projectWorkflowId: 3});

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects/8/project-workflows/3');
    });
});
