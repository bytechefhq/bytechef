import {WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import EmbeddedWorkflowExecutionsTable from './EmbeddedWorkflowExecutionsTable';

vi.mock('./EmbeddedWorkflowExecutionsDropdownMenu', () => ({
    default: () => <button>execution menu</button>,
}));

vi.mock('../stores/useWorkflowExecutionSheetStore', () => ({
    default: () => ({
        setWorkflowExecutionId: vi.fn(),
        setWorkflowExecutionSheetOpen: vi.fn(),
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
});
