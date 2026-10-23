import {render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowExecutionSheetContent from '../WorkflowExecutionSheetContent';

const hoisted = vi.hoisted(() => ({
    embeddedGranted: false,
    taskExecutionQuery: vi.fn(),
    workspaceGranted: false,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => hoisted.workspaceGranted,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', () => ({
    useHasEmbeddedScope: () => hoisted.embeddedGranted,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetWorkflowExecutionTaskExecutionQuery: (...args: unknown[]) => hoisted.taskExecutionQuery(...args),
}));

vi.mock('@/components/ui/resizable', () => ({
    ResizableHandle: () => <div />,
    ResizablePanel: ({children}: {children: ReactNode}) => <div>{children}</div>,
    ResizablePanelGroup: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/components/ui/scroll-area', () => ({
    ScrollArea: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsHeader', () => ({default: () => <div />}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/components/workflow-executions/WorkflowTaskExecutionItem', () => ({default: () => <div />}));

vi.mock('@/shared/components/workflow-executions/WorkflowTriggerExecutionItem', () => ({default: () => <div />}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel', () => ({
    default: ({detailsHidden}: {detailsHidden?: boolean}) => (
        <div data-testid="tabs-panel">{`detailsHidden=${String(detailsHidden)}`}</div>
    ),
}));

const job = {id: '1', status: 'COMPLETED', taskExecutions: [{id: '11', name: 'task'}]} as never;

const renderContent = (embedded?: boolean) =>
    render(
        <WorkflowExecutionSheetContent
            activeTab="input"
            deepestFailedExecution={null}
            dialogOpen={false}
            embedded={embedded}
            handleBreadcrumbNavigate={vi.fn()}
            handleSeeExecutions={vi.fn()}
            handleTaskClick={vi.fn()}
            isTriggerExecution={false}
            job={job}
            jobFailedWithNoExecutions={false}
            jobFailureError={{message: '', stackTrace: []} as never}
            selectedItem={{id: '11'} as never}
            setActiveTab={vi.fn()}
            setDialogOpen={vi.fn()}
            subflowStack={[]}
            taskExecutionAttempts={[]}
            workflowExecutionId={1}
        />
    );

const lastQueryEnabledArgument = () => {
    const calls = hoisted.taskExecutionQuery.mock.calls;

    return calls[calls.length - 1][1];
};

describe('automation WorkflowExecutionSheetContent', () => {
    beforeEach(() => {
        hoisted.embeddedGranted = false;
        hoisted.workspaceGranted = false;

        hoisted.taskExecutionQuery.mockReset();
        hoisted.taskExecutionQuery.mockReturnValue({data: undefined, isLoading: false});
    });

    it('gates details on the workspace scope on the automation page', () => {
        hoisted.workspaceGranted = true;
        hoisted.embeddedGranted = false;

        renderContent();

        expect(screen.getByTestId('tabs-panel')).toHaveTextContent('detailsHidden=false');
        expect(lastQueryEnabledArgument()).toBe(true);
    });

    it('hides details on the automation page without the workspace scope even with the embedded scope', () => {
        hoisted.workspaceGranted = false;
        hoisted.embeddedGranted = true;

        renderContent();

        expect(screen.getByTestId('tabs-panel')).toHaveTextContent('detailsHidden=true');
        expect(lastQueryEnabledArgument()).toBe(false);
    });

    it('gates details on the embedded scope when shown from the embedded executions page', () => {
        hoisted.workspaceGranted = false;
        hoisted.embeddedGranted = true;

        renderContent(true);

        expect(screen.getByTestId('tabs-panel')).toHaveTextContent('detailsHidden=false');
        expect(lastQueryEnabledArgument()).toBe(true);
    });

    it('hides details on the embedded executions page without the embedded scope even with the workspace scope', () => {
        hoisted.workspaceGranted = true;
        hoisted.embeddedGranted = false;

        renderContent(true);

        expect(screen.getByTestId('tabs-panel')).toHaveTextContent('detailsHidden=true');
        expect(lastQueryEnabledArgument()).toBe(false);
    });
});
