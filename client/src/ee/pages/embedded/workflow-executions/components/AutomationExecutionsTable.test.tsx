import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationExecutionsTable from './AutomationExecutionsTable';

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

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    useGetConnectedUserQuery: (id: number, enabled?: boolean) => ({
        data: enabled ? {externalId: `external-${id}`, id} : undefined,
    }),
}));

vi.mock('@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore', () => ({
    default: () => ({
        setWorkflowExecutionId: hoisted.setWorkflowExecutionId,
        setWorkflowExecutionSheetOpen: hoisted.setWorkflowExecutionSheetOpen,
    }),
}));

const data = [
    {
        connectedUserId: 42,
        id: 3,
        job: {
            endDate: new Date('2026-10-20T10:00:02Z'),
            id: '3',
            startDate: new Date('2026-10-20T10:00:00Z'),
            status: 'COMPLETED',
        },
        project: {name: 'Order Processing'},
        projectDeployment: {name: '__EMBEDDED__acme-user-42__PRODUCTION', projectVersion: 4},
        workflow: {label: 'Heavy orders load test'},
    },
] as unknown as WorkflowExecution[];

describe('AutomationExecutionsTable', () => {
    beforeEach(() => {
        hoisted.setWorkflowExecutionId.mockClear();
        hoisted.setWorkflowExecutionSheetOpen.mockClear();
    });

    it('renders the workflow, duration and a pinned Actions column', () => {
        render(<AutomationExecutionsTable data={data} />);

        expect(screen.getByText('Heavy orders load test')).toBeInTheDocument();
        expect(screen.getByText('Connected user')).toBeInTheDocument();
        expect(screen.getByText('2000ms')).toBeInTheDocument();
        expect(screen.getByText('Actions').closest('th')).toHaveClass('sticky', 'right-0');
    });

    it('renders the project of each run before its connected user', () => {
        render(<AutomationExecutionsTable data={data} />);

        const headers = screen.getAllByRole('columnheader').map((header) => header.textContent);

        expect(headers.indexOf('Project')).toBe(headers.indexOf('Connected user') - 1);
        expect(screen.getByRole('cell', {name: 'Order Processing'})).toBeInTheDocument();
    });

    it('renders the connected user of each run instead of its internal deployment name', () => {
        render(<AutomationExecutionsTable data={data} />);

        expect(screen.getByRole('cell', {name: 'external-42'})).toBeInTheDocument();
        expect(screen.queryByText('__EMBEDDED__acme-user-42__PRODUCTION')).not.toBeInTheDocument();
    });

    it("renders each run's version from its deployment", () => {
        render(<AutomationExecutionsTable data={data} />);

        expect(screen.getByText('Version')).toBeInTheDocument();
        expect(screen.getByText('V4')).toBeInTheDocument();
    });

    it('opens the execution sheet when a row is clicked', async () => {
        render(<AutomationExecutionsTable data={data} />);

        await userEvent.click(screen.getByText('Heavy orders load test'));

        expect(hoisted.setWorkflowExecutionId).toHaveBeenCalledWith(3);
        expect(hoisted.setWorkflowExecutionSheetOpen).toHaveBeenCalledWith(true);
    });

    it('opens the execution sheet from the View action', async () => {
        render(<AutomationExecutionsTable data={data} />);

        await userEvent.click(screen.getByText('execution menu'));

        expect(hoisted.setWorkflowExecutionId).toHaveBeenCalledTimes(1);
        expect(hoisted.setWorkflowExecutionSheetOpen).toHaveBeenCalledWith(true);
    });
});
