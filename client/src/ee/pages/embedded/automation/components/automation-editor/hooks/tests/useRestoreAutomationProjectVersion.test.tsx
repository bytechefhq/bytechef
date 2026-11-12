import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useRestoreAutomationProjectVersion} from '../useRestoreAutomationProjectVersion';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
    restoreMutate: vi.fn(),
    toast: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useRestoreAutomationProjectVersionMutation: () => ({isPending: false, mutate: hoisted.restoreMutate}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('sonner', () => ({
    toast: hoisted.toast,
}));

const createQueryClientWrapper = (workflowUuids: string[]) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    queryClient.setQueryData(['automationProjects'], {
        automationProjects: [
            {
                id: 'project-1',
                workflowTemplates: workflowUuids.map((workflowUuid) => ({workflowUuid})),
            },
        ],
    });

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return {queryClient, wrapper};
};

const restoreVersion = ({projectId, workflowUuids}: {projectId?: string; workflowUuids: string[]}) => {
    const {queryClient, wrapper} = createQueryClientWrapper(workflowUuids);

    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');
    const onSuccess = vi.fn();

    hoisted.restoreMutate.mockImplementation((_variables, options) => options.onSuccess());

    const {result} = renderHook(
        () => useRestoreAutomationProjectVersion({currentWorkflowUuid: 'workflow-1', projectId}),
        {
            wrapper,
        }
    );

    act(() => {
        result.current.handleRestoreProjectVersion(2, onSuccess);
    });

    return {invalidateQueriesSpy, onSuccess};
};

describe('useRestoreAutomationProjectVersion', () => {
    beforeEach(() => {
        hoisted.navigate.mockReset();
        hoisted.restoreMutate.mockReset();
        hoisted.toast.mockReset();

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'aiAgent_1'} as ReturnType<
                typeof useWorkflowNodeDetailsPanelStore.getState
            >['currentNode'],
        });
    });

    it('restores the version and refreshes the project, its versions and its draft changes', async () => {
        const {invalidateQueriesSpy, onSuccess} = restoreVersion({
            projectId: 'project-1',
            workflowUuids: ['workflow-1', 'workflow-2'],
        });

        await waitFor(() => {
            expect(onSuccess).toHaveBeenCalledTimes(1);
        });

        expect(hoisted.restoreMutate).toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 2},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['automationProjects']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['automationProjectVersions']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['automationProjectVersionWorkflowChanges']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['automationProjectVersionWorkflowChange']});
        expect(hoisted.toast).toHaveBeenCalledWith('Version V2 applied to the draft.');
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
        expect(hoisted.navigate).not.toHaveBeenCalled();
    });

    it('opens the first workflow of the restored draft when the current workflow is not part of it', async () => {
        const {onSuccess} = restoreVersion({projectId: 'project-1', workflowUuids: ['workflow-2', 'workflow-3']});

        await waitFor(() => {
            expect(onSuccess).toHaveBeenCalledTimes(1);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations/workflow-2/editor');
    });

    it('returns to the automations list when the restored draft has no workflows', async () => {
        const {onSuccess} = restoreVersion({projectId: 'project-1', workflowUuids: []});

        await waitFor(() => {
            expect(onSuccess).toHaveBeenCalledTimes(1);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations');
    });

    it('restores nothing until the project is known', () => {
        const {onSuccess} = restoreVersion({workflowUuids: ['workflow-1']});

        expect(hoisted.restoreMutate).not.toHaveBeenCalled();
        expect(onSuccess).not.toHaveBeenCalled();
    });
});
