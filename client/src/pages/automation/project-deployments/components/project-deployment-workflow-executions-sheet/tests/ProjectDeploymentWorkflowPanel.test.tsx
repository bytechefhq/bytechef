import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentWorkflowPanel from '../ProjectDeploymentWorkflowPanel';

const {editorPropsMock, layoutState} = vi.hoisted(() => ({
    editorPropsMock: vi.fn(),
    layoutState: {componentsIsLoading: false},
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowLayout', () => ({
    useWorkflowLayout: () => ({
        componentDefinitions: layoutState.componentsIsLoading ? undefined : [],
        componentsError: null,
        componentsIsLoading: layoutState.componentsIsLoading,
        taskDispatcherDefinitions: [],
        taskDispatcherDefinitionsError: null,
        taskDispatcherDefinitionsLoading: false,
    }),
}));

vi.mock('@/pages/automation/workflow-executions/hooks/useWorkflowExecutionSheetWorkflowPanel', () => ({
    default: () => ({canvasWidth: 640, rootDivRef: {current: null}}),
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowEditor', () => ({
    default: (props: {customCanvasWidth: number; fitViewOnLoad: boolean; readOnlyWorkflow: Workflow}) => {
        editorPropsMock(props);

        return <div>{`Workflow editor ${props.readOnlyWorkflow.id}`}</div>;
    },
}));

const workflow = {id: 'workflow1', label: 'workflow1'} as Workflow;

describe('ProjectDeploymentWorkflowPanel', () => {
    beforeEach(() => {
        editorPropsMock.mockReset();

        useWorkflowDataStore.getState().reset();

        layoutState.componentsIsLoading = false;
    });

    it('renders the workflow read-only, fitted to the panel width', async () => {
        render(<ProjectDeploymentWorkflowPanel workflow={workflow} />);

        expect(await screen.findByText('Workflow editor workflow1')).toBeInTheDocument();
        expect(editorPropsMock).toHaveBeenLastCalledWith(
            expect.objectContaining({customCanvasWidth: 640, fitViewOnLoad: true, readOnlyWorkflow: workflow})
        );
    });

    it('does not render the editor while component definitions are loading', async () => {
        layoutState.componentsIsLoading = true;

        render(<ProjectDeploymentWorkflowPanel workflow={workflow} />);

        await new Promise((resolve) => requestAnimationFrame(() => resolve(undefined)));

        expect(screen.queryByText('Workflow editor workflow1')).not.toBeInTheDocument();
        expect(editorPropsMock).not.toHaveBeenCalled();
    });

    it('gives the workflow editor underneath its workflow and canvas back when it unmounts', async () => {
        const editorNodes = [{data: {}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'}] as Node[];
        const editorWorkflow = {id: 'editorWorkflow', nodeNames: ['trigger_1']};

        useWorkflowDataStore.setState({isWorkflowLoaded: true, nodes: editorNodes, workflow: editorWorkflow});

        const {unmount} = render(<ProjectDeploymentWorkflowPanel workflow={workflow} />);

        await screen.findByText('Workflow editor workflow1');

        useWorkflowDataStore.getState().setNodes([]);

        unmount();

        const {isWorkflowLoaded, nodes, workflow: storeWorkflow} = useWorkflowDataStore.getState();

        expect(isWorkflowLoaded).toBe(true);
        expect(storeWorkflow).toBe(editorWorkflow);
        expect(nodes).toBe(editorNodes);
    });

    it('resets the shared workflow data store when it unmounts and no editor workflow was loaded', async () => {
        const {unmount} = render(<ProjectDeploymentWorkflowPanel workflow={workflow} />);

        await screen.findByText('Workflow editor workflow1');

        useWorkflowDataStore.getState().setNodes([{data: {}, id: 'trigger_1', position: {x: 0, y: 0}}]);

        unmount();

        expect(useWorkflowDataStore.getState().nodes).toEqual([]);
        expect(useWorkflowDataStore.getState().isWorkflowLoaded).toBe(false);
    });
});
