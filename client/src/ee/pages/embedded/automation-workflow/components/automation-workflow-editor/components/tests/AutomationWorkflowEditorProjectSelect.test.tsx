import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorProjectSelect from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorProjectSelect';
import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ComponentProps} from 'react';
import {beforeEach, expect, it, vi} from 'vitest';

type ProjectsPropType = ComponentProps<typeof AutomationWorkflowEditorProjectSelect>['projects'];

mockScrollIntoView();
windowResizeObserver();

const mockSetSelectedProjectId = vi.fn();

const longProjectName = 'A project with a name long enough to need a tooltip';

const mockProjects = [
    {id: 'project-1', name: 'Mailing', workflowTemplates: []},
    {id: 'project-2', name: 'Reporting', workflowTemplates: []},
    {id: 'project-3', name: longProjectName, workflowTemplates: []},
] as never as ProjectsPropType;

const renderProjectSelect = (props: Partial<ComponentProps<typeof AutomationWorkflowEditorProjectSelect>> = {}) =>
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorProjectSelect
                projectId="project-1"
                projects={mockProjects}
                selectedProjectId="project-1"
                setSelectedProjectId={mockSetSelectedProjectId}
                {...props}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

it('reads "Current project" while the current project is selected', () => {
    renderProjectSelect();

    expect(screen.getByRole('combobox', {name: 'Select project'})).toHaveTextContent('Current project');
});

it('reads "All projects" while every project is selected', () => {
    renderProjectSelect({selectedProjectId: '0'});

    expect(screen.getByRole('combobox', {name: 'Select project'})).toHaveTextContent('All projects');
});

it('shows the name of another selected project', () => {
    renderProjectSelect({selectedProjectId: 'project-2'});

    expect(screen.getByRole('combobox', {name: 'Select project'})).toHaveTextContent('Reporting');
});

it('shows the full name of a long selected project', () => {
    renderProjectSelect({selectedProjectId: 'project-3'});

    expect(screen.getByRole('combobox', {name: 'Select project'})).toHaveTextContent(longProjectName);
});

it('lists the pinned options and the projects when opened', async () => {
    renderProjectSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));

    expect(screen.getByPlaceholderText('Search projects...')).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Current project'})).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'All projects'})).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Reporting'})).toBeInTheDocument();
});

it('omits the Current project option before the current project is known', async () => {
    renderProjectSelect({projectId: '', selectedProjectId: ''});

    await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));

    expect(screen.queryByRole('option', {name: 'Current project'})).not.toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'All projects'})).toBeInTheDocument();
});

it('filters the projects by the typed text', async () => {
    renderProjectSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));
    await userEvent.type(screen.getByPlaceholderText('Search projects...'), 'rep');

    expect(screen.getByRole('option', {name: 'Reporting'})).toBeInTheDocument();
    expect(screen.queryByRole('option', {name: 'Mailing'})).not.toBeInTheDocument();
});

it('selects a project by id', async () => {
    renderProjectSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));
    await userEvent.click(screen.getByRole('option', {name: 'Reporting'}));

    expect(mockSetSelectedProjectId).toHaveBeenCalledWith('project-2');
});

it('selects every project through the All projects option', async () => {
    renderProjectSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));
    await userEvent.click(screen.getByRole('option', {name: 'All projects'}));

    expect(mockSetSelectedProjectId).toHaveBeenCalledWith('0');
});
