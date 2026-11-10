import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationEditorBreadcrumb from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorBreadcrumb';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ComponentProps} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {expect, it, vi} from 'vitest';

type ProjectPropType = ComponentProps<typeof AutomationEditorBreadcrumb>['project'];

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
        <MemoryRouter>
            <TooltipProvider>
                <AutomationEditorBreadcrumb project={draftProject} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByRole('heading', {name: 'Mailing'})).toBeInTheDocument();
    expect(screen.getByText('V3')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
});

it('shows the draft being edited, not the last published version, of a published project', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AutomationEditorBreadcrumb
                    project={{...draftProject, lastPublishedVersion: 2, published: true} as never as ProjectPropType}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByText('V3')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
    expect(screen.queryByText('PUBLISHED')).not.toBeInTheDocument();
});

it('shows the item switcher after a separator when one is given', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AutomationEditorBreadcrumb itemSelect={<button>Mailer</button>} project={draftProject} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByRole('button', {name: 'Mailer'})).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
});

it('omits the separator and switcher without an item switcher', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AutomationEditorBreadcrumb project={draftProject} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getAllByRole('listitem')).toHaveLength(2);
});

it('links back to the Automations list page first', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AutomationEditorBreadcrumb project={draftProject} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByRole('link', {name: 'Automations'})).toHaveAttribute('href', '/embedded/automations');
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('Automations');
});

it('switches to another project from the title select', async () => {
    const onProjectValueChange = vi.fn();

    render(
        <MemoryRouter>
            <TooltipProvider>
                <AutomationEditorBreadcrumb
                    onProjectValueChange={onProjectValueChange}
                    project={draftProject}
                    projects={[
                        {...draftProject, workflowTemplates: [{workflowUuid: 'uuid-1'}]} as never as ProjectPropType,
                        {
                            ...draftProject,
                            id: 'project-2',
                            lastPublishedVersion: 4,
                            name: 'Billing',
                            workflowTemplates: [{workflowUuid: 'uuid-2'}],
                        } as never as ProjectPropType,
                        {
                            ...draftProject,
                            id: 'project-3',
                            name: 'Empty',
                            workflowTemplates: [],
                        } as never as ProjectPropType,
                    ]}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

    await userEvent.click(screen.getByLabelText('Project select'));

    expect(screen.getByRole('menuitemradio', {name: 'Mailing'})).toHaveAttribute('data-state', 'checked');
    expect(screen.getByRole('menuitemradio', {name: 'Empty'})).toHaveAttribute('data-disabled');

    expect(screen.getByRole('menuitemradio', {name: /Billing/})).toContainElement(screen.getByText('Published V4'));
    expect(screen.getAllByText(/Published/)).toHaveLength(1);

    await userEvent.click(screen.getByRole('menuitemradio', {name: /Billing/}));

    expect(onProjectValueChange).toHaveBeenCalledWith('project-2');
});
