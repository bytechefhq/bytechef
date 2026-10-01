import {TooltipProvider} from '@/components/ui/tooltip';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {NodeDataType} from '@/shared/types';
import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useClusterElementsCanvasDialogStore} from '../components/stores/useClusterElementsCanvasDialogStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import ClusterFrameShell from './ClusterFrameShell';

vi.mock('@xyflow/react', () => ({
    Handle: () => null,
    Position: {Bottom: 'bottom', Left: 'left', Right: 'right', Top: 'top'},
}));

const {editorContext, saveWorkflowDefinitionMock, updateWorkflowMutationMock} = vi.hoisted(() => {
    const mutation = {isPending: false, mutate: vi.fn()};

    return {
        editorContext: {updateWorkflowMutation: mutation as typeof mutation | undefined},
        saveWorkflowDefinitionMock: vi.fn(),
        updateWorkflowMutationMock: mutation,
    };
});

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => editorContext,
}));

vi.mock('../utils/saveWorkflowDefinition', () => ({default: saveWorkflowDefinitionMock}));

const CLUSTER_ROOT_DATA = {
    clusterFrame: {clusterRootId: 'aiAgent_1', height: 320, width: 640},
    label: 'AI Agent',
    workflowNodeName: 'aiAgent_1',
} as unknown as NodeDataType;

const AI_AGENT_ROOT_DATA = {
    ...CLUSTER_ROOT_DATA,
    componentName: 'aiAgent',
    name: 'aiAgent_1',
} as unknown as NodeDataType;

function buildWorkflowWithClusterRoot(): void {
    useWorkflowDataStore.setState({
        workflow: {
            definition: JSON.stringify({
                tasks: [
                    {
                        clusterElements: {
                            model: {
                                metadata: {ui: {nodePosition: {x: 40, y: 20}}},
                                name: 'model_1',
                                type: 'openAiChat/v1',
                            },
                        },
                        name: 'aiAgent_1',
                        type: 'aiAgent/v1',
                    },
                ],
            }),
            id: 'workflow-1',
            nodeNames: [],
            tasks: [{clusterRoot: true, name: 'aiAgent_1', type: 'aiAgent/v1'}],
            version: 1,
        },
    });
}

describe('ClusterFrameShell', () => {
    beforeEach(() => {
        editorContext.updateWorkflowMutation = updateWorkflowMutationMock;
        saveWorkflowDefinitionMock.mockReset();

        useWorkflowEditorStore.setState({
            clusterElementsCanvasOpen: false,
            clusterFrameLockedByRootId: {},
            rootClusterElementNodeData: undefined,
        });
        useWorkflowDataStore.setState({workflow: {definition: undefined, id: 'workflow-1', nodeNames: [], version: 1}});
        useClusterElementsCanvasDialogStore.setState({
            showAiAgentEditor: false,
            showDataStreamEditor: false,
            testingPanelOpen: false,
        });
        applicationInfoStore.setState((state) => ({
            ai: {...state.ai, copilot: {enabled: false}},
            featureFlags: {},
        }));
        useCopilotPanelStore.setState({copilotPanelOpen: false});
        useCopilotStore.setState({
            context: {mode: useCopilotStore.getState().context.mode, parameters: {}, source: Source.WORKFLOW_EDITOR},
        });
    });

    it('paints the box at the size the pre-pass computed', () => {
        render(
            <ClusterFrameShell data={CLUSTER_ROOT_DATA} nodeId="aiAgent_1">
                <div>root card</div>
            </ClusterFrameShell>
        );

        const shell = screen.getByTestId('cluster-frame-shell');

        expect(shell).toHaveStyle({height: '320px', width: '640px'});
        expect(screen.getByText('root card')).toBeInTheDocument();
    });

    it('renders the children bare when no box has been computed', () => {
        render(
            <ClusterFrameShell data={{label: 'AI Agent'} as NodeDataType} nodeId="aiAgent_1">
                <div>root card</div>
            </ClusterFrameShell>
        );

        expect(screen.queryByTestId('cluster-frame-shell')).not.toBeInTheDocument();
        expect(screen.getByText('root card')).toBeInTheDocument();
    });

    it('renders the lock and reset header controls, starting locked', () => {
        render(
            <ClusterFrameShell data={CLUSTER_ROOT_DATA} nodeId="aiAgent_1">
                <div>root card</div>
            </ClusterFrameShell>
        );

        expect(screen.getByRole('button', {name: 'Unlock node movement'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Reset layout'})).toBeInTheDocument();
    });

    it('leaves the header controls out when there is no mutation to edit the workflow with', () => {
        editorContext.updateWorkflowMutation = undefined;

        render(
            <ClusterFrameShell data={CLUSTER_ROOT_DATA} nodeId="aiAgent_1">
                <div>root card</div>
            </ClusterFrameShell>
        );

        expect(screen.queryByRole('button', {name: 'Unlock node movement'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Reset layout'})).not.toBeInTheDocument();
        expect(screen.getByText('AI Agent')).toBeInTheDocument();
    });

    it('unlocks and relocks this root only when the lock control is clicked', () => {
        render(
            <ClusterFrameShell data={CLUSTER_ROOT_DATA} nodeId="aiAgent_1">
                <div>root card</div>
            </ClusterFrameShell>
        );

        act(() => {
            screen.getByRole('button', {name: 'Unlock node movement'}).click();
        });

        expect(useWorkflowEditorStore.getState().clusterFrameLockedByRootId.aiAgent_1).toBe(false);
        expect(screen.getByRole('button', {name: 'Lock node movement'})).toBeInTheDocument();

        act(() => {
            screen.getByRole('button', {name: 'Lock node movement'}).click();
        });

        expect(useWorkflowEditorStore.getState().clusterFrameLockedByRootId.aiAgent_1).toBe(true);
    });

    it('clears every saved element position for this root when Reset layout is used', () => {
        buildWorkflowWithClusterRoot();

        render(
            <ClusterFrameShell data={CLUSTER_ROOT_DATA} nodeId="aiAgent_1">
                <div>root card</div>
            </ClusterFrameShell>
        );

        act(() => {
            screen.getByRole('button', {name: 'Reset layout'}).click();
        });

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);

        const [[callArguments]] = saveWorkflowDefinitionMock.mock.calls;

        expect(callArguments.updateWorkflowMutation).toBe(updateWorkflowMutationMock);
        expect(callArguments.nodeData.workflowNodeName).toBe('aiAgent_1');
        expect(callArguments.nodeData.componentName).toBe('aiAgent');
        expect(callArguments.nodeData.clusterElements.model.metadata.ui.nodePosition).toBeUndefined();
    });

    it('does nothing when Reset layout is used with no cluster root task in the definition', () => {
        render(
            <ClusterFrameShell data={CLUSTER_ROOT_DATA} nodeId="aiAgent_1">
                <div>root card</div>
            </ClusterFrameShell>
        );

        act(() => {
            screen.getByRole('button', {name: 'Reset layout'}).click();
        });

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });

    it('opens the AI Agent editor preset, not the canvas view', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <ClusterFrameShell data={AI_AGENT_ROOT_DATA} nodeId="aiAgent_1">
                    <div>root card</div>
                </ClusterFrameShell>
            </TooltipProvider>
        );

        await user.click(screen.getByLabelText('Switch to AI Agent editor'));

        expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(true);
        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(true);
        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData?.workflowNodeName).toBe('aiAgent_1');
    });

    it('opens the playground without opening the dialog', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <ClusterFrameShell data={AI_AGENT_ROOT_DATA} nodeId="aiAgent_1">
                    <div>root card</div>
                </ClusterFrameShell>
            </TooltipProvider>
        );

        await user.click(screen.getByLabelText('Test agent'));

        expect(useClusterElementsCanvasDialogStore.getState().testingPanelOpen).toBe(true);
        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(false);
        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData?.workflowNodeName).toBe('aiAgent_1');
    });

    it('opens Copilot through the fresh-context contract, tagging the agent this button was clicked on', async () => {
        applicationInfoStore.setState((state) => ({
            ai: {...state.ai, copilot: {enabled: true}},
            featureFlags: {...state.featureFlags, 'ff-1570': true, 'ff-4070': true},
        }));

        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <ClusterFrameShell data={AI_AGENT_ROOT_DATA} nodeId="aiAgent_1">
                    <div>root card</div>
                </ClusterFrameShell>
            </TooltipProvider>
        );

        await user.click(screen.getByLabelText('Open Copilot panel'));

        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
        expect(useCopilotStore.getState().context.source).toBe(Source.CLUSTER_ELEMENT);
        expect(useCopilotStore.getState().context.parameters.taskName).toBe('aiAgent_1');
        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData?.workflowNodeName).toBe('aiAgent_1');
    });
});
