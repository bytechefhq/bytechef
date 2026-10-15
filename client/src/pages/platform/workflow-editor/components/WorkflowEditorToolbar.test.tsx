import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactFlowProvider} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowEditorProvider, WorkflowEditorStateI, WorkflowMockProvider} from '../providers/workflowEditorProvider';
import useClusterElementsViewModeStore from '../stores/useClusterElementsViewModeStore';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {clearAllWorkflowMutations} from '../utils/workflowMutationGuard';
import WorkflowEditorToolbar from './WorkflowEditorToolbar';

const renderToolbar = (readOnly = false) =>
    render(
        <TooltipProvider>
            <ReactFlowProvider>
                <WorkflowMockProvider>
                    <WorkflowEditorToolbar readOnly={readOnly} />
                </WorkflowMockProvider>
            </ReactFlowProvider>
        </TooltipProvider>
    );

describe('WorkflowEditorToolbar - lock button', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({edges: [], nodes: []});
        useWorkflowEditorStore.setState({nodesLocked: true});
    });

    it('renders the unlock affordance when locked and not read-only', () => {
        renderToolbar(false);

        expect(screen.getByLabelText('Unlock node movement')).toBeInTheDocument();
    });

    it('hides the lock button in read-only mode', () => {
        renderToolbar(true);

        expect(screen.queryByLabelText('Unlock node movement')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Lock node movement')).not.toBeInTheDocument();
    });

    it('toggles nodesLocked and the label when clicked', async () => {
        const user = userEvent.setup();

        renderToolbar(false);

        await user.click(screen.getByLabelText('Unlock node movement'));

        expect(useWorkflowEditorStore.getState().nodesLocked).toBe(false);
        expect(screen.getByLabelText('Lock node movement')).toBeInTheDocument();
    });
});

describe('WorkflowEditorToolbar - layout direction button', () => {
    const mutateMock = vi.fn();

    const renderEditorToolbar = (readOnly = false) =>
        render(
            <TooltipProvider>
                <ReactFlowProvider>
                    <WorkflowEditorProvider
                        value={{updateWorkflowMutation: {mutate: mutateMock}} as unknown as WorkflowEditorStateI}
                    >
                        <WorkflowEditorToolbar readOnly={readOnly} />
                    </WorkflowEditorProvider>
                </ReactFlowProvider>
            </TooltipProvider>
        );

    beforeEach(() => {
        clearAllWorkflowMutations();
        mutateMock.mockReset();

        useLayoutDirectionStore.setState({
            currentWorkflowUuid: 'workflow-uuid-1',
            directionsByWorkflowUuid: {},
            layoutDirection: 'TB',
        });

        useWorkflowDataStore.setState((state) => ({
            edges: [],
            nodes: [],
            workflow: {
                ...state.workflow,
                definition: JSON.stringify({label: 'Workflow', tasks: []}),
                id: 'workflow_1',
                version: 1,
            },
        }));
    });

    it('saves the new direction into the workflow definition', async () => {
        const user = userEvent.setup();

        renderEditorToolbar(false);

        await user.click(screen.getByLabelText('Switch to horizontal layout'));

        expect(useLayoutDirectionStore.getState().layoutDirection).toBe('LR');
        expect(screen.getByLabelText('Switch to vertical layout')).toBeInTheDocument();

        const storedDefinition = useWorkflowDataStore.getState().workflow.definition!;

        expect(JSON.parse(storedDefinition).metadata.ui.layoutDirection).toBe('LR');
        expect(mutateMock).toHaveBeenCalledTimes(1);
        expect(mutateMock.mock.calls[0][0].workflow.definition).toBe(storedDefinition);
    });

    it('only switches the canvas direction in read-only mode', async () => {
        const definition = useWorkflowDataStore.getState().workflow.definition;
        const user = userEvent.setup();

        renderEditorToolbar(true);

        await user.click(screen.getByLabelText('Switch to horizontal layout'));

        expect(useLayoutDirectionStore.getState().layoutDirection).toBe('LR');
        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definition);
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('does not save without an update mutation', async () => {
        const definition = useWorkflowDataStore.getState().workflow.definition;
        const user = userEvent.setup();

        renderToolbar(false);

        await user.click(screen.getByLabelText('Switch to horizontal layout'));

        expect(useLayoutDirectionStore.getState().layoutDirection).toBe('LR');
        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definition);
    });
});

describe('WorkflowEditorToolbar - layout engine button', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({edges: [], nodes: []});
        useLayoutEngineStore.setState({layoutEngine: 'dagre'});
    });

    it('renders enabled for a condition-only workflow and toggles the engine', async () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
                    id: 'condition_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
        });

        const user = userEvent.setup();

        renderToolbar(false);

        const layoutEngineButton = screen.getByLabelText('Switch to experimental layout engine');

        expect(layoutEngineButton).toBeEnabled();

        await user.click(layoutEngineButton);

        expect(useLayoutEngineStore.getState().layoutEngine).toBe('elk');
        expect(screen.getByLabelText('Switch to standard layout engine')).toBeInTheDocument();
    });

    it('is disabled when the workflow contains an unknown dispatcher', () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {componentName: 'mystery-dispatcher', taskDispatcher: true, taskDispatcherId: 'mystery_1'},
                    id: 'mystery_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
        });

        renderToolbar(false);

        expect(screen.getByLabelText('Switch to experimental layout engine')).toBeDisabled();
    });

    it('stays enabled for cluster-root workflows', () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {clusterRoot: true, componentName: 'aiAgent', workflowNodeName: 'aiAgent_1'},
                    id: 'aiAgent_1',
                    position: {x: 0, y: 0},
                    type: 'clusterRoot',
                },
            ],
        });

        renderToolbar(false);

        expect(screen.getByLabelText('Switch to experimental layout engine')).toBeEnabled();
    });
});

describe('WorkflowEditorToolbar - cluster elements view mode button', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({edges: [], nodes: []});
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});
    });

    it('offers the switch to inline box mode while the dialog is in force', () => {
        renderToolbar(false);

        expect(screen.getByLabelText('Show cluster elements inline')).toBeInTheDocument();
    });

    it('switches the stored mode to box and offers the way back', async () => {
        const user = userEvent.setup();

        renderToolbar(false);

        await user.click(screen.getByLabelText('Show cluster elements inline'));

        expect(useClusterElementsViewModeStore.getState().clusterElementsViewMode).toBe('box');
        expect(screen.getByLabelText('Show cluster elements in the editor dialog')).toBeInTheDocument();
    });

    it('switches back to the dialog from box mode', async () => {
        const user = userEvent.setup();

        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});

        renderToolbar(false);

        await user.click(screen.getByLabelText('Show cluster elements in the editor dialog'));

        expect(useClusterElementsViewModeStore.getState().clusterElementsViewMode).toBe('dialog');
    });
});
