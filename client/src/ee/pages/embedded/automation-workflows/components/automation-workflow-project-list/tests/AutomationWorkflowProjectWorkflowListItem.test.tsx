import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowProjectWorkflowListItem from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectWorkflowListItem';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

const workflow = {
    components: [{icon: '<svg />', name: 'gmail', title: 'Gmail'}],
    description: 'Sends mail',
    label: 'Mailer',
    lastModifiedDate: '2026-02-11T09:30:00Z',
    triggers: [{icon: '<svg />', name: 'manual', title: 'Manual'}],
    workflowUuid: 'wf-1',
};

const renderRow = () =>
    render(
        <TooltipProvider>
            <ul>
                <AutomationWorkflowProjectWorkflowListItem
                    onDeleteWorkflow={vi.fn()}
                    onSelectWorkflow={vi.fn()}
                    workflow={workflow}
                />
            </ul>
        </TooltipProvider>
    );

describe('AutomationWorkflowProjectWorkflowListItem', () => {
    it('renders the label, the trigger badge, and the modified date', () => {
        renderRow();

        expect(screen.getByText('Mailer')).toBeInTheDocument();
        expect(screen.getByText('Manual')).toBeInTheDocument();
        expect(screen.getByText(/Modified at/)).toBeInTheDocument();
    });
});
