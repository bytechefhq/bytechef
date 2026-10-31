import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowExecutionSheetWorkflowPanel from '../WorkflowExecutionSheetWorkflowPanel';

const {workflowEditorMock} = vi.hoisted(() => ({
    workflowEditorMock: vi.fn<(props: Record<string, unknown>) => ReactNode>(() => (
        <div data-testid="workflow-editor" />
    )),
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowEditor', () => ({default: workflowEditorMock}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowLayout', () => ({
    useWorkflowLayout: () => ({
        componentDefinitions: [],
        componentsError: null,
        componentsIsLoading: false,
        taskDispatcherDefinitions: [],
        taskDispatcherDefinitionsError: null,
        taskDispatcherDefinitionsLoading: false,
    }),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({
        data: {definition: '{"metadata":{"ui":{"layoutDirection":"LR"}}}', id: 'workflow1'},
        isLoading: false,
    }),
}));

vi.mock('../../../hooks/useWorkflowExecutionSheetWorkflowPanel', () => ({
    default: () => ({canvasWidth: 800, rootDivRef: {current: null}}),
}));

vi.mock('@/components/PageLoader', () => ({default: ({children}: {children: ReactNode}) => <>{children}</>}));

describe('WorkflowExecutionSheetWorkflowPanel', () => {
    beforeEach(() => {
        useWorkflowDataStore.getState().reset();
    });

    it('renders the execution flow top-to-bottom regardless of the layout direction saved with the workflow', async () => {
        render(
            <WorkflowExecutionSheetWorkflowPanel
                workflowExecution={{id: 1, workflow: {id: 'workflow1'}} as WorkflowExecution}
            />
        );

        await screen.findByTestId('workflow-editor');

        expect(workflowEditorMock.mock.lastCall?.[0]).toMatchObject({readOnlyLayoutDirection: 'TB'});
    });

    it('gives the workflow editor underneath its workflow and canvas back when the panel closes', async () => {
        const editorNodes = [{data: {}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'}] as Node[];
        const editorWorkflow = {id: 'editorWorkflow', nodeNames: ['trigger_1']};

        useWorkflowDataStore.setState({isWorkflowLoaded: true, nodes: editorNodes, workflow: editorWorkflow});

        const {unmount} = render(
            <WorkflowExecutionSheetWorkflowPanel
                workflowExecution={{id: 1, workflow: {id: 'workflow1'}} as WorkflowExecution}
            />
        );

        await screen.findByTestId('workflow-editor');

        useWorkflowDataStore.getState().setNodes([]);

        unmount();

        const {isWorkflowLoaded, nodes, workflow} = useWorkflowDataStore.getState();

        expect(isWorkflowLoaded).toBe(true);
        expect(workflow).toBe(editorWorkflow);
        expect(nodes).toBe(editorNodes);
    });
});
