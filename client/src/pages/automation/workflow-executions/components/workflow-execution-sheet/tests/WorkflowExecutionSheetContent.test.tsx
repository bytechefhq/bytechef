import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowExecutionSheetContent from '../WorkflowExecutionSheetContent';

const {queryMock} = vi.hoisted(() => ({
    queryMock: vi.fn(() => ({data: undefined, isLoading: false})),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
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

const renderContent = (selectedItem: object) =>
    render(
        <WorkflowExecutionSheetContent
            activeTab="input"
            deepestFailedExecution={null}
            dialogOpen={false}
            handleBreadcrumbNavigate={vi.fn()}
            handleSeeExecutions={vi.fn()}
            handleTaskClick={vi.fn()}
            isTriggerExecution={false}
            jobFailedWithNoExecutions={false}
            jobFailureError={{message: 'failed', stackTrace: []}}
            selectedItem={selectedItem as never}
            setActiveTab={vi.fn()}
            setDialogOpen={vi.fn()}
            subflowStack={[]}
            taskExecutionAttempts={[]}
            workflowExecutionId={7}
        />
    );

describe('automation WorkflowExecutionSheetContent', () => {
    beforeEach(() => {
        queryMock.mockClear();
    });

    it('does not request the task execution of a tool call row', () => {
        renderContent({clusterElementCallId: 'call-uuid', id: 'call-uuid'});

        expect(queryMock).toHaveBeenCalledWith(expect.anything(), false, false);
        expect(queryMock).not.toHaveBeenCalledWith(expect.anything(), true, false);
    });

    it('requests the task execution of a normal task', () => {
        renderContent({id: '42'});

        expect(queryMock).toHaveBeenCalledWith({id: 7, taskExecutionId: 42}, true, false);
    });
});
