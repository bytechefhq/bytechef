import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorBreadcrumb from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorBreadcrumb';
import {render, screen} from '@/shared/util/test-utils';
import {ComponentProps} from 'react';
import {expect, it} from 'vitest';

type ProjectPropType = ComponentProps<typeof AutomationWorkflowEditorBreadcrumb>['project'];

const draftProject = {
    id: 'project-1',
    lastPublishedVersion: null,
    name: 'Mailing',
    published: false,
    version: 3,
    workflowTemplates: [],
} as never as ProjectPropType;

it('shows the project title with its draft version and status', () => {
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorBreadcrumb project={draftProject} />
        </TooltipProvider>
    );

    expect(screen.getByRole('heading', {name: 'Mailing'})).toBeInTheDocument();
    expect(screen.getByText('V3')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
});

it('shows the last published version of a published project', () => {
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorBreadcrumb
                project={{...draftProject, lastPublishedVersion: 2, published: true} as never as ProjectPropType}
            />
        </TooltipProvider>
    );

    expect(screen.getByText('V2')).toBeInTheDocument();
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
});

it('shows the item switcher after a separator when one is given', () => {
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorBreadcrumb itemSelect={<button>Mailer</button>} project={draftProject} />
        </TooltipProvider>
    );

    expect(screen.getByRole('button', {name: 'Mailer'})).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
});

it('omits the separator and switcher without an item switcher', () => {
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorBreadcrumb project={draftProject} />
        </TooltipProvider>
    );

    expect(screen.getAllByRole('listitem')).toHaveLength(1);
});
