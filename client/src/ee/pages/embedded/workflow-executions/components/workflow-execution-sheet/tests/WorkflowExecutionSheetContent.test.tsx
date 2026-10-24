import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetContent from '../WorkflowExecutionSheetContent';

const {queryMock} = vi.hoisted(() => ({
    queryMock: vi.fn(() => ({data: undefined, isLoading: false})),
}));

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', () => ({
    useGetWorkflowExecutionTaskExecutionQuery: queryMock,
}));

vi.mock('@/components/ui/resizable', () => ({
    ResizableHandle: () => null,
    ResizablePanel: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    ResizablePanelGroup: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsHeader', () => ({default: () => null}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem', () => ({default: () => null}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel', () => ({default: () => null}));

describe('embedded WorkflowExecutionSheetContent', () => {
    beforeEach(() => {
        queryMock.mockClear();

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 1, workflowExecutionSheetOpen: true});
    });

    it('does not request the task execution of a tool call row', () => {
        render(
            <WorkflowExecutionSheetContent
                job={
                    {
                        id: '1',
                        status: 'COMPLETED',
                        taskExecutions: [{clusterElementCallId: 'call-uuid', id: 'call-uuid'}],
                    } as never
                }
            />
        );

        expect(queryMock).toHaveBeenCalledWith(expect.anything(), false, false);
        expect(queryMock).not.toHaveBeenCalledWith(expect.anything(), true, false);
    });

    it('requests the task execution of a normal task', () => {
        render(
            <WorkflowExecutionSheetContent
                job={{id: '1', status: 'COMPLETED', taskExecutions: [{id: '42'}]} as never}
            />
        );

        expect(queryMock).toHaveBeenCalledWith({id: 1, taskExecutionId: 42}, true, false);
    });
});
