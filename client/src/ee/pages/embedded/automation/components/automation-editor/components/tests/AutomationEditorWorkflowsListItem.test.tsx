import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationEditorWorkflowsListItem from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorWorkflowsListItem';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
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
    lastExecutionDate: null,
    lastModifiedDate: '2026-02-11T09:30:00Z',
    permissionExpression: null,
    triggers: [],
    workflowId: 'draft-wf-1',
    workflowUuid: 'wf-1',
};

const project = {
    categoryId: null,
    codeWorkflowDraftPublishable: null,
    codeWorkflowLanguage: null,
    codeWorkflowProject: false,
    configurationCount: 0,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
    lastPublishedVersion: null,
    name: 'Project One',
    permissionExpression: null,
    published: false,
    tagIds: [],
    version: 1,
    workflowTemplates: [workflow],
};

describe('AutomationEditorWorkflowsListItem', () => {
    it('renders the workflow label and the edited date without component icons', () => {
        render(
            <MemoryRouter>
                <TooltipProvider>
                    <ul>
                        <AutomationEditorWorkflowsListItem
                            currentWorkflowId="wf-other"
                            onWorkflowClick={vi.fn()}
                            project={project}
                            workflow={workflow}
                        />
                    </ul>
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(screen.getByText('Mailer')).toBeInTheDocument();
        expect(screen.getByText('Edited')).toBeInTheDocument();
        expect(screen.getByText(`on ${new Date('2026-02-11T09:30:00Z').toLocaleDateString()}`)).toBeInTheDocument();
        expect(screen.queryByText(/Edited on/)).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Workflow component icon')).not.toBeInTheDocument();
    });

    it('shows how long ago a recently edited workflow was edited', () => {
        const recentlyEditedWorkflow = {
            ...workflow,
            lastExecutionDate: null,
            lastModifiedDate: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
        };

        render(
            <MemoryRouter>
                <TooltipProvider>
                    <ul>
                        <AutomationEditorWorkflowsListItem
                            currentWorkflowId="wf-other"
                            onWorkflowClick={vi.fn()}
                            project={project}
                            workflow={recentlyEditedWorkflow}
                        />
                    </ul>
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(screen.getByText('Edited')).toBeInTheDocument();
        expect(screen.getByText('5 minutes ago')).toBeInTheDocument();
    });

    it('marks the card as current when the ids match', () => {
        const {container} = render(
            <MemoryRouter>
                <TooltipProvider>
                    <ul>
                        <AutomationEditorWorkflowsListItem
                            currentWorkflowId="wf-1"
                            onWorkflowClick={vi.fn()}
                            project={project}
                            workflow={workflow}
                        />
                    </ul>
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(container.querySelector('li')).toHaveClass('border-stroke-brand-primary');
    });

    it('calls onWorkflowClick with the workflow uuid when the card is clicked', () => {
        const onWorkflowClick = vi.fn();

        render(
            <MemoryRouter>
                <TooltipProvider>
                    <ul>
                        <AutomationEditorWorkflowsListItem
                            currentWorkflowId="wf-other"
                            onWorkflowClick={onWorkflowClick}
                            project={project}
                            workflow={workflow}
                        />
                    </ul>
                </TooltipProvider>
            </MemoryRouter>
        );

        fireEvent.click(screen.getByText('Mailer'));

        expect(onWorkflowClick).toHaveBeenCalledWith('wf-1');
    });
});
