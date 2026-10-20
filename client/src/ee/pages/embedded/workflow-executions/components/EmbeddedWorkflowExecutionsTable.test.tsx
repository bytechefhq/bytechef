import {WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import EmbeddedWorkflowExecutionsTable from './EmbeddedWorkflowExecutionsTable';

vi.mock('./EmbeddedWorkflowExecutionsDropdownMenu', () => ({
    default: () => <button>execution menu</button>,
}));

const hoisted = vi.hoisted(() => ({
    setWorkflowExecutionId: vi.fn(),
    setWorkflowExecutionSheetOpen: vi.fn(),
}));

vi.mock('../stores/useWorkflowExecutionSheetStore', () => ({
    default: () => ({
        setWorkflowExecutionId: hoisted.setWorkflowExecutionId,
        setWorkflowExecutionSheetOpen: hoisted.setWorkflowExecutionSheetOpen,
    }),
}));

const data = [
    {
        id: 1,
        integration: {componentName: 'googleMail'},
        integrationInstanceConfiguration: {integrationVersion: 5, name: 'Gmail Code'},
        job: {
            endDate: new Date('2026-10-20T10:00:05Z'),
            id: '1',
            startDate: new Date('2026-10-20T10:00:00Z'),
            status: 'FAILED',
        },
        workflow: {label: 'Notify Slack on New Email'},
    },
] as unknown as WorkflowExecution[];

describe('EmbeddedWorkflowExecutionsTable', () => {
    it('renders a pinned Actions column with the execution menu', () => {
        render(<EmbeddedWorkflowExecutionsTable data={data} />);

        expect(screen.getByText('Actions').closest('th')).toHaveClass('sticky', 'right-0');
        expect(screen.getByText('execution menu').closest('td')).toHaveClass('sticky', 'right-0');
    });

    it('stripes rows like the automation executions table', () => {
        render(<EmbeddedWorkflowExecutionsTable data={data} />);

        expect(screen.getByText('Notify Slack on New Email').closest('tr')).toHaveClass(
            'even:bg-surface-neutral-secondary'
        );
    });

    it('renders the integration, instance, version and duration columns', () => {
        render(<EmbeddedWorkflowExecutionsTable data={data} />);

        expect(screen.getByText('googleMail')).toBeInTheDocument();
        expect(screen.getByText('Gmail Code')).toBeInTheDocument();
        expect(screen.getByText('V5')).toBeInTheDocument();
        expect(screen.getByText('5000ms')).toBeInTheDocument();
    });

    it('opens the execution sheet when a row is clicked', async () => {
        render(<EmbeddedWorkflowExecutionsTable data={data} />);

        await userEvent.click(screen.getByText('Notify Slack on New Email'));

        expect(hoisted.setWorkflowExecutionId).toHaveBeenCalledWith(1);
        expect(hoisted.setWorkflowExecutionSheetOpen).toHaveBeenCalledWith(true);
    });
});
