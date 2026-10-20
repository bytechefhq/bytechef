import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowExecutionsTable from './AutomationWorkflowExecutionsTable';

const hoisted = vi.hoisted(() => ({
    setWorkflowExecutionId: vi.fn(),
    setWorkflowExecutionSheetOpen: vi.fn(),
}));

vi.mock('./EmbeddedWorkflowExecutionsDropdownMenu', () => ({
    default: ({onView}: {onView: () => void}) => (
        <button
            onClick={(event) => {
                event.stopPropagation();

                onView();
            }}
        >
            execution menu
        </button>
    ),
}));

vi.mock('@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore', () => ({
    default: () => ({
        setWorkflowExecutionId: hoisted.setWorkflowExecutionId,
        setWorkflowExecutionSheetOpen: hoisted.setWorkflowExecutionSheetOpen,
    }),
}));

const data = [
    {
        id: 3,
        job: {
            endDate: new Date('2026-10-20T10:00:02Z'),
            id: '3',
            startDate: new Date('2026-10-20T10:00:00Z'),
            status: 'COMPLETED',
        },
        projectDeployment: {name: 'Load Test (acme-user-42)'},
        workflow: {label: 'Heavy orders load test'},
    },
] as unknown as WorkflowExecution[];

describe('AutomationWorkflowExecutionsTable', () => {
    beforeEach(() => {
        hoisted.setWorkflowExecutionId.mockClear();
        hoisted.setWorkflowExecutionSheetOpen.mockClear();
    });

    it('renders the workflow, duration and a pinned Actions column', () => {
        render(<AutomationWorkflowExecutionsTable data={data} />);

        expect(screen.getByText('Heavy orders load test')).toBeInTheDocument();
        expect(screen.getByText('Connected user')).toBeInTheDocument();
        expect(screen.getByText('2000ms')).toBeInTheDocument();
        expect(screen.getByText('Actions').closest('th')).toHaveClass('sticky', 'right-0');
    });

    it('opens the execution sheet when a row is clicked', async () => {
        render(<AutomationWorkflowExecutionsTable data={data} />);

        await userEvent.click(screen.getByText('Heavy orders load test'));

        expect(hoisted.setWorkflowExecutionId).toHaveBeenCalledWith(3);
        expect(hoisted.setWorkflowExecutionSheetOpen).toHaveBeenCalledWith(true);
    });

    it('opens the execution sheet from the View action', async () => {
        render(<AutomationWorkflowExecutionsTable data={data} />);

        await userEvent.click(screen.getByText('execution menu'));

        expect(hoisted.setWorkflowExecutionId).toHaveBeenCalledTimes(1);
        expect(hoisted.setWorkflowExecutionSheetOpen).toHaveBeenCalledWith(true);
    });
});
