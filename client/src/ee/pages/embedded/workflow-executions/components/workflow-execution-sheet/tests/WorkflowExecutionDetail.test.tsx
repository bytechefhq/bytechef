import {useWorkflowExecutionDetailQueries} from '@/shared/components/workflow-executions/providers/workflowExecutionDetailQueriesProvider';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowExecutionDetail, {embeddedWorkflowExecutionDetailQueries} from '../WorkflowExecutionDetail';

const hoisted = vi.hoisted(() => ({
    integrationWorkflowExecutionQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/shared/queries/embedded/workflowExecutions.queries')>()),
    useGetIntegrationWorkflowExecutionQuery: hoisted.integrationWorkflowExecutionQuery,
}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail', () => ({
    default: function AutomationWorkflowExecutionDetailMock({
        enabled,
        workflowExecutionId,
    }: {
        enabled?: boolean;
        workflowExecutionId: number;
    }) {
        const {useGetWorkflowExecutionQuery} = useWorkflowExecutionDetailQueries();

        useGetWorkflowExecutionQuery({id: workflowExecutionId, lazy: true}, enabled);

        return <div>{`Automation detail ${workflowExecutionId} ${String(enabled)}`}</div>;
    },
}));

describe('embedded WorkflowExecutionDetail', () => {
    it('renders the automation detail view against the embedded execution queries', () => {
        hoisted.integrationWorkflowExecutionQuery.mockReturnValue({data: undefined});

        render(<WorkflowExecutionDetail enabled={false} workflowExecutionId={7} />);

        expect(screen.getByText('Automation detail 7 false')).toBeInTheDocument();
        expect(hoisted.integrationWorkflowExecutionQuery).toHaveBeenCalledWith({id: 7, lazy: true}, false, undefined);
    });

    it('scopes the task execution children cache to the embedded execution keys', () => {
        expect(embeddedWorkflowExecutionDetailQueries.taskExecutionChildrenOfJobQueryKey(7)).toEqual([
            'integrationWorkflowExecutions',
            7,
            'taskExecutionChildren',
        ]);
    });
});
