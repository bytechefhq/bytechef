import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ChangeEvent, ReactNode} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useCreateAutomationWorkflowProjectWorkflow} from '../useCreateAutomationWorkflowProjectWorkflow';

type MutationOptionsType = {onSuccess?: (data: {createAutomationWorkflowProjectWorkflow: string}) => void};

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAutomationWorkflowProjectWorkflowMutation: () => ({mutate: hoisted.mutate}),
}));

const renderUseCreateAutomationWorkflowProjectWorkflow = ({
    bottomResizablePanelRef,
    projectId = 'project-1',
}: {
    bottomResizablePanelRef?: {current: Pick<PanelImperativeHandle, 'resize'> | null};
    projectId?: string;
} = {}) => {
    const queryClient = new QueryClient();
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const {result} = renderHook(
        () =>
            useCreateAutomationWorkflowProjectWorkflow({
                bottomResizablePanelRef: bottomResizablePanelRef as {current: PanelImperativeHandle | null},
                projectId,
            }),
        {wrapper}
    );

    return {invalidateQueriesSpy, result};
};

const completeMutation = (workflowUuid: string) => {
    const mutationOptions = hoisted.mutate.mock.calls[0][1] as MutationOptionsType;

    mutationOptions.onSuccess?.({createAutomationWorkflowProjectWorkflow: workflowUuid});
};

const createFileChangeEvent = (file?: File) => {
    const target = {files: file ? [file] : [], value: 'C:\\fakepath\\workflow.json'};

    return {event: {target} as unknown as ChangeEvent<HTMLInputElement>, target};
};

describe('useCreateAutomationWorkflowProjectWorkflow', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useWorkflowEditorStore.setState({showBottomPanel: true});
    });

    it('creates an empty workflow in the project from the dialog values', () => {
        const {result} = renderUseCreateAutomationWorkflowProjectWorkflow();

        result.current.createWorkflow({
            description: 'Sends mail',
            label: 'Mailer',
            permissionExpression: "hasRole('ADMIN')",
        });

        const [variables] = hoisted.mutate.mock.calls[0];

        expect(variables.projectId).toBe('project-1');
        expect(variables.permissionExpression).toBe("hasRole('ADMIN')");
        expect(JSON.parse(variables.definition)).toEqual({
            description: 'Sends mail',
            inputs: [],
            label: 'Mailer',
            tasks: [],
            triggers: [],
        });
    });

    it('refreshes the projects, collapses the Test Output and opens the created workflow', () => {
        const resize = vi.fn();
        const {invalidateQueriesSpy, result} = renderUseCreateAutomationWorkflowProjectWorkflow({
            bottomResizablePanelRef: {current: {resize}},
        });

        result.current.createWorkflow({description: '', label: 'Mailer', permissionExpression: ''});

        completeMutation('workflow-uuid-42');

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['automationWorkflowProjects']});
        expect(useWorkflowEditorStore.getState().showBottomPanel).toBe(false);
        expect(resize).toHaveBeenCalledWith(0);
        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automation-workflows/workflow-uuid-42/editor');
    });

    it('does not create a workflow before the project is known', () => {
        const {result} = renderUseCreateAutomationWorkflowProjectWorkflow({projectId: ''});

        result.current.createWorkflow({description: '', label: 'Mailer', permissionExpression: ''});

        expect(hoisted.mutate).not.toHaveBeenCalled();
    });

    it('imports the chosen workflow file into the project and opens it', async () => {
        const {result} = renderUseCreateAutomationWorkflowProjectWorkflow();

        const {event, target} = createFileChangeEvent(
            new File(['{"label":"Imported"}'], 'workflow.json', {type: 'application/json'})
        );

        await act(async () => {
            await result.current.handleWorkflowFileChange(event);
        });

        expect(target.value).toBe('');
        expect(hoisted.mutate).toHaveBeenCalledWith(
            {definition: '{"label":"Imported"}', projectId: 'project-1'},
            expect.anything()
        );

        completeMutation('workflow-uuid-7');

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automation-workflows/workflow-uuid-7/editor');
    });

    it('ignores an import without a chosen file', async () => {
        const {result} = renderUseCreateAutomationWorkflowProjectWorkflow();

        const {event} = createFileChangeEvent();

        await act(async () => {
            await result.current.handleWorkflowFileChange(event);
        });

        expect(hoisted.mutate).not.toHaveBeenCalled();
    });
});
