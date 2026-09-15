import WorkflowEditorReadOnlyProvider from '@/pages/platform/workflow-editor/providers/WorkflowEditorReadOnlyProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import handleDeleteTrigger from '@/pages/platform/workflow-editor/utils/handleDeleteTrigger';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import saveWorkflowDefinitionUpdate from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinitionUpdate';
import {
    addStickyNote,
    deleteStickyNote,
    updateStickyNote,
} from '@/pages/platform/workflow-editor/utils/stickyNoteUtils';
import {
    isWorkflowEditorReadOnly,
    setWorkflowEditorReadOnly,
} from '@/pages/platform/workflow-editor/utils/workflowEditorReadOnlyGuard';
import {setWorkflowMutating} from '@/pages/platform/workflow-editor/utils/workflowMutationGuard';
import {render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const flushMutationQueue = () => new Promise((resolve) => setTimeout(resolve, 0));

const STICKY_NOTE_DEFINITION = JSON.stringify({
    metadata: {
        ui: {
            stickyNotes: [{color: 'yellow', content: 'note', id: 'stickyNote_1', position: {x: 0, y: 0}}],
        },
    },
    tasks: [],
});

const TWO_TRIGGER_WORKFLOW = {
    definition: JSON.stringify({
        tasks: [],
        triggers: [
            {name: 'trigger_1', type: 'manual/v1/manual'},
            {name: 'trigger_2', type: 'manual/v1/manual'},
        ],
    }),
    id: 'workflow-1',
    nodeNames: [],
    triggers: [
        {name: 'trigger_1', type: 'manual/v1/manual'},
        {name: 'trigger_2', type: 'manual/v1/manual'},
    ],
    version: 1,
};

const createMutation = () => ({
    isPending: false,
    mutate: vi.fn(),
    mutateAsync: vi.fn().mockResolvedValue({parameters: {}}),
});

describe('workflowEditorReadOnlyGuard', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {definition: '{"tasks":[]}', id: 'workflow-1', version: 1},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'httpClient_1', parameters: {}, workflowNodeName: 'httpClient_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    afterEach(() => {
        setWorkflowEditorReadOnly(false);
        setWorkflowMutating('workflow-1', false);
    });

    it('is set by the read-only provider while mounted and cleared on unmount', () => {
        const {unmount} = render(
            <WorkflowEditorReadOnlyProvider readOnly>
                <span>viewer</span>
            </WorkflowEditorReadOnlyProvider>
        );

        expect(isWorkflowEditorReadOnly()).toBe(true);

        unmount();

        expect(isWorkflowEditorReadOnly()).toBe(false);
    });

    it('stays clear under an editable provider', () => {
        render(
            <WorkflowEditorReadOnlyProvider readOnly={false}>
                <span>editor</span>
            </WorkflowEditorReadOnlyProvider>
        );

        expect(isWorkflowEditorReadOnly()).toBe(false);
    });

    it('lets saveProperty write when the editor is editable', async () => {
        const updateWorkflowNodeParameterMutation = createMutation();

        saveProperty({
            path: 'uri',
            type: 'STRING',
            updateWorkflowNodeParameterMutation: updateWorkflowNodeParameterMutation as never,
            value: 'https://example.com',
            workflowId: 'workflow-1',
        });

        await flushMutationQueue();

        expect(updateWorkflowNodeParameterMutation.mutateAsync).toHaveBeenCalledTimes(1);
    });

    it('turns saveProperty into a no-op in read-only mode', async () => {
        const updateWorkflowNodeParameterMutation = createMutation();

        setWorkflowEditorReadOnly(true);

        saveProperty({
            path: 'uri',
            type: 'STRING',
            updateWorkflowNodeParameterMutation: updateWorkflowNodeParameterMutation as never,
            value: 'https://example.com',
            workflowId: 'workflow-1',
        });

        await flushMutationQueue();

        expect(updateWorkflowNodeParameterMutation.mutateAsync).not.toHaveBeenCalled();
    });

    it('lets deleteProperty write when the editor is editable', async () => {
        const deleteWorkflowNodeParameterMutation = createMutation();

        deleteProperty('workflow-1', 'uri', deleteWorkflowNodeParameterMutation as never);

        await flushMutationQueue();

        expect(deleteWorkflowNodeParameterMutation.mutateAsync).toHaveBeenCalledTimes(1);
    });

    it('turns deleteProperty into a no-op in read-only mode', async () => {
        const deleteWorkflowNodeParameterMutation = createMutation();

        setWorkflowEditorReadOnly(true);

        deleteProperty('workflow-1', 'uri', deleteWorkflowNodeParameterMutation as never);

        await flushMutationQueue();

        expect(deleteWorkflowNodeParameterMutation.mutateAsync).not.toHaveBeenCalled();
    });

    it('lets saveWorkflowDefinitionUpdate write when the editor is editable', () => {
        const updateWorkflowMutation = createMutation();

        saveWorkflowDefinitionUpdate({
            updateDefinition: (workflowDefinition) => ({...workflowDefinition, inputs: []}),
            updateWorkflowMutation: updateWorkflowMutation as never,
        });

        expect(updateWorkflowMutation.mutate).toHaveBeenCalledTimes(1);
    });

    it('turns saveWorkflowDefinitionUpdate into a no-op in read-only mode', () => {
        const updateWorkflowMutation = createMutation();

        setWorkflowEditorReadOnly(true);

        saveWorkflowDefinitionUpdate({
            updateDefinition: (workflowDefinition) => ({...workflowDefinition, inputs: []}),
            updateWorkflowMutation: updateWorkflowMutation as never,
        });

        expect(updateWorkflowMutation.mutate).not.toHaveBeenCalled();
    });

    it('lets sticky-note edits write when the editor is editable', () => {
        const updateWorkflowMutation = createMutation();

        useWorkflowDataStore.setState({
            workflow: {definition: STICKY_NOTE_DEFINITION, id: 'workflow-1', version: 1},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        updateStickyNote({
            id: 'stickyNote_1',
            patch: {content: 'edited'},
            updateWorkflowMutation: updateWorkflowMutation as never,
        });

        expect(updateWorkflowMutation.mutate).toHaveBeenCalledTimes(1);
    });

    it('turns sticky-note add, update and delete into no-ops in read-only mode', () => {
        const updateWorkflowMutation = createMutation();

        useWorkflowDataStore.setState({
            workflow: {definition: STICKY_NOTE_DEFINITION, id: 'workflow-1', version: 1},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        setWorkflowEditorReadOnly(true);

        addStickyNote({position: {x: 10, y: 10}, updateWorkflowMutation: updateWorkflowMutation as never});
        updateStickyNote({
            id: 'stickyNote_1',
            patch: {content: 'edited'},
            updateWorkflowMutation: updateWorkflowMutation as never,
        });
        deleteStickyNote({id: 'stickyNote_1', updateWorkflowMutation: updateWorkflowMutation as never});

        expect(updateWorkflowMutation.mutate).not.toHaveBeenCalled();
        expect(useWorkflowDataStore.getState().workflow.definition).toBe(STICKY_NOTE_DEFINITION);
    });

    it('lets handleDeleteTrigger write when the editor is editable', () => {
        const updateWorkflowMutation = createMutation();

        handleDeleteTrigger({
            cancelWorkflowQueries: vi.fn(),
            invalidateWorkflowQueries: vi.fn(),
            triggerName: 'trigger_1',
            updateWorkflowMutation: updateWorkflowMutation as never,
            workflow: TWO_TRIGGER_WORKFLOW as never,
        });

        expect(updateWorkflowMutation.mutate).toHaveBeenCalledTimes(1);
    });

    it('turns handleDeleteTrigger into a no-op in read-only mode', () => {
        const updateWorkflowMutation = createMutation();

        setWorkflowEditorReadOnly(true);

        handleDeleteTrigger({
            cancelWorkflowQueries: vi.fn(),
            invalidateWorkflowQueries: vi.fn(),
            triggerName: 'trigger_1',
            updateWorkflowMutation: updateWorkflowMutation as never,
            workflow: TWO_TRIGGER_WORKFLOW as never,
        });

        expect(updateWorkflowMutation.mutate).not.toHaveBeenCalled();
    });
});
