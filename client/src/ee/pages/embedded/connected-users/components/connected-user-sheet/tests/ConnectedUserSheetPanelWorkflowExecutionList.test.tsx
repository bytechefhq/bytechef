import ConnectedUserSheetPanelWorkflowExecutionList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowExecutionList';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    anyWorkflowExecutionsTable: vi.fn(),
    useWorkflowExecutions: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions', () => ({
    useWorkflowExecutions: hoisted.useWorkflowExecutions,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/AnyWorkflowExecutionsTable', () => ({
    default: (props: {data: unknown[]; showConnectedUser?: boolean}) => {
        hoisted.anyWorkflowExecutionsTable(props);

        return <div>{`${props.data.length} executions`}</div>;
    },
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => <div>integration execution sheet</div>,
}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => <div>automation execution sheet</div>,
}));

const integrationExecution = {
    integrationWorkflowExecution: {id: 1, workflow: {label: 'Send welcome email'}},
    source: 'INTEGRATION',
};

const workflowExecutionsResult = (overrides: object) => ({
    refetchWorkflowExecutions: vi.fn(),
    workflowExecutionPage: undefined,
    workflowExecutionsError: null,
    workflowExecutionsIsLoading: false,
    ...overrides,
});

describe('ConnectedUserSheetPanelWorkflowExecutionList', () => {
    beforeEach(() => {
        hoisted.anyWorkflowExecutionsTable.mockReset();
        hoisted.useWorkflowExecutions.mockReset();
    });

    it("reads the connected user's executions from every source", () => {
        hoisted.useWorkflowExecutions.mockReturnValue(workflowExecutionsResult({workflowExecutionsIsLoading: true}));

        render(<ConnectedUserSheetPanelWorkflowExecutionList connectedUserId={7} />);

        expect(hoisted.useWorkflowExecutions).toHaveBeenCalledWith(
            'ANY',
            expect.objectContaining({connectedUserId: 7, pageNumber: 0})
        );
        expect(screen.getByRole('status', {name: 'Loading workflow executions'})).toBeInTheDocument();
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });

    it('lists the executions without a connected user column and mounts the execution sheets', () => {
        hoisted.useWorkflowExecutions.mockReturnValue(
            workflowExecutionsResult({
                workflowExecutionPage: {content: [integrationExecution], size: 20, totalElements: 1, totalPages: 1},
            })
        );

        render(<ConnectedUserSheetPanelWorkflowExecutionList connectedUserId={7} />);

        expect(screen.getByText('1 executions')).toBeInTheDocument();
        expect(hoisted.anyWorkflowExecutionsTable).toHaveBeenCalledWith(
            expect.objectContaining({showConnectedUser: false})
        );
        expect(screen.getByText('integration execution sheet')).toBeInTheDocument();
        expect(screen.getByText('automation execution sheet')).toBeInTheDocument();
        expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    });

    it('requests the next page when a page link is clicked', async () => {
        hoisted.useWorkflowExecutions.mockReturnValue(
            workflowExecutionsResult({
                workflowExecutionPage: {content: [integrationExecution], size: 1, totalElements: 2, totalPages: 2},
            })
        );

        render(<ConnectedUserSheetPanelWorkflowExecutionList connectedUserId={7} />);

        await userEvent.click(screen.getByText('2'));

        expect(hoisted.useWorkflowExecutions).toHaveBeenLastCalledWith(
            'ANY',
            expect.objectContaining({connectedUserId: 7, pageNumber: 1})
        );
    });

    it('shows an empty state with a docs link when the user has no executions', () => {
        hoisted.useWorkflowExecutions.mockReturnValue(
            workflowExecutionsResult({workflowExecutionPage: {content: [], totalPages: 0}})
        );

        render(<ConnectedUserSheetPanelWorkflowExecutionList connectedUserId={7} />);

        expect(screen.getByText('No workflow executions yet')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: /How workflow executions work/})).toHaveAttribute(
            'href',
            'https://docs.bytechef.io/platform/embedded/monitor/workflow-executions'
        );
    });

    it('offers a retry when the executions cannot be loaded', async () => {
        const refetchWorkflowExecutions = vi.fn();

        hoisted.useWorkflowExecutions.mockReturnValue(
            workflowExecutionsResult({refetchWorkflowExecutions, workflowExecutionsError: new Error('boom')})
        );

        render(<ConnectedUserSheetPanelWorkflowExecutionList connectedUserId={7} />);

        expect(screen.getByText('Workflow executions could not be loaded.')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(refetchWorkflowExecutions).toHaveBeenCalled();
    });
});
