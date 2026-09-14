import {Workflow} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentWorkflowPanel from '../ProjectDeploymentWorkflowPanel';

const {editorPropsMock, layoutState, resetMock} = vi.hoisted(() => ({
    editorPropsMock: vi.fn(),
    layoutState: {componentsIsLoading: false},
    resetMock: vi.fn(),
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

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: {getState: () => ({reset: resetMock})},
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
        resetMock.mockReset();

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

    it('resets the shared workflow data store when it unmounts', async () => {
        const {unmount} = render(<ProjectDeploymentWorkflowPanel workflow={workflow} />);

        await screen.findByText('Workflow editor workflow1');

        expect(resetMock).not.toHaveBeenCalled();

        unmount();

        expect(resetMock).toHaveBeenCalledTimes(1);
    });
});
