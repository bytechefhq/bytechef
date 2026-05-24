import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorWorkflowsListItem from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItem';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

const workflow = {
    components: [
        {
            icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"></svg>',
            name: 'gmail',
            title: 'Gmail',
        },
    ],
    description: 'Sends mail',
    label: 'Mailer',
    lastModifiedDate: '2026-02-11T09:30:00Z',
    triggers: [],
    workflowUuid: 'wf-1',
};

describe('AutomationWorkflowEditorWorkflowsListItem', () => {
    it('renders the workflow label, the edited date, and a component icon', () => {
        render(
            <TooltipProvider>
                <ul>
                    <AutomationWorkflowEditorWorkflowsListItem
                        currentWorkflowId="wf-other"
                        onWorkflowClick={vi.fn()}
                        workflow={workflow}
                    />
                </ul>
            </TooltipProvider>
        );

        expect(screen.getByText('Mailer')).toBeInTheDocument();
        expect(screen.getByText(/Edited/)).toBeInTheDocument();
        expect(screen.getByText(new Date('2026-02-11T09:30:00Z').toLocaleDateString())).toBeInTheDocument();
        expect(screen.getByTitle('Gmail')).toBeInTheDocument();
    });

    it('marks the card as current when the ids match', () => {
        const {container} = render(
            <TooltipProvider>
                <ul>
                    <AutomationWorkflowEditorWorkflowsListItem
                        currentWorkflowId="wf-1"
                        onWorkflowClick={vi.fn()}
                        workflow={workflow}
                    />
                </ul>
            </TooltipProvider>
        );

        expect(container.querySelector('li')).toHaveClass('border-stroke-brand-primary');
    });

    it('calls onWorkflowClick with the workflow uuid when the card is clicked', () => {
        const onWorkflowClick = vi.fn();

        const {container} = render(
            <TooltipProvider>
                <ul>
                    <AutomationWorkflowEditorWorkflowsListItem
                        currentWorkflowId="wf-other"
                        onWorkflowClick={onWorkflowClick}
                        workflow={workflow}
                    />
                </ul>
            </TooltipProvider>
        );

        fireEvent.click(container.querySelector('li')!);

        expect(onWorkflowClick).toHaveBeenCalledWith('wf-1');
    });
});
