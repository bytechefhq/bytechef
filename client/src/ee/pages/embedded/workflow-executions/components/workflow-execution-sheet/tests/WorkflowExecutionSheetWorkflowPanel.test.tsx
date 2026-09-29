import {WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

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

vi.mock('@/ee/shared/queries/embedded/workflows.queries', () => ({
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
    it('renders the execution flow top-to-bottom regardless of the layout direction saved with the workflow', async () => {
        render(
            <WorkflowExecutionSheetWorkflowPanel
                workflowExecution={{id: 1, workflow: {id: 'workflow1'}} as WorkflowExecution}
            />
        );

        await screen.findByTestId('workflow-editor');

        expect(workflowEditorMock.mock.lastCall?.[0]).toMatchObject({readOnlyLayoutDirection: 'TB'});
    });
});
