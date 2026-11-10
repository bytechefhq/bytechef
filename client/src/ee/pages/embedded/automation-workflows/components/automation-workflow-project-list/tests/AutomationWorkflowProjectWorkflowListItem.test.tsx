import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowProjectWorkflowListItem from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectWorkflowListItem';
import {resetAll, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const handlers = {
    onDeleteWorkflow: vi.fn(),
    onEditWorkflow: vi.fn(),
    onSelectWorkflow: vi.fn(),
};

const workflow = {
    components: [{icon: '<svg />', name: 'gmail', title: 'Gmail'}],
    description: 'Sends mail',
    label: 'Mailer',
    lastModifiedDate: '2026-02-11T09:30:00Z',
    permissionExpression: null,
    triggers: [{icon: '<svg />', name: 'manual', title: 'Manual'}],
    workflowId: 'draft-wf-1',
    workflowUuid: 'wf-1',
};

const renderRow = () =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <TooltipProvider>
                <ul>
                    <AutomationWorkflowProjectWorkflowListItem {...handlers} workflow={workflow} />
                </ul>
            </TooltipProvider>
        </QueryClientProvider>
    );

const openMenu = async () => {
    renderRow();

    await userEvent.click(screen.getByRole('button', {name: 'Workflow Actions'}));
};

describe('AutomationWorkflowProjectWorkflowListItem', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('renders the label, the trigger badge, and the modified date', () => {
        renderRow();

        expect(screen.getByText('Mailer')).toBeInTheDocument();
        expect(screen.getByText('Manual')).toBeInTheDocument();
        expect(screen.getByText(/Modified at/)).toBeInTheDocument();
    });

    it('opens the workflow editor when the row is clicked', async () => {
        renderRow();

        await userEvent.click(screen.getByText('Mailer'));

        expect(handlers.onSelectWorkflow).toHaveBeenCalledWith('wf-1');
    });

    describe('menu', () => {
        it('should render with the design-system DropdownMenu', async () => {
            await openMenu();

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Edit Workflow'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'Delete Workflow'})).toHaveClass(
                'text-content-destructive-primary'
            );
        });

        it('should edit the workflow when Edit is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Edit Workflow'}));

            expect(handlers.onEditWorkflow).toHaveBeenCalledWith(workflow);
        });

        it('does not offer an Executions action', async () => {
            await openMenu();

            expect(screen.queryByRole('menuitem', {name: 'Workflow Executions'})).not.toBeInTheDocument();
        });

        it('should delete the workflow when Delete is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Delete Workflow'}));

            expect(handlers.onDeleteWorkflow).toHaveBeenCalledWith('wf-1');
        });
    });
});
