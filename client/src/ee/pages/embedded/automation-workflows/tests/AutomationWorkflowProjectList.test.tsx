import {TooltipProvider} from '@/components/ui/tooltip';
import {AutomationWorkflowProjectTagsQuery, AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {createTestQueryClientWrapper, screen, userEvent} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {ReactElement, ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectList from '../components/automation-workflow-project-list/AutomationWorkflowProjectList';

const renderWithProviders = (ui: ReactElement) => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientWrapper>
            <TooltipProvider>{children}</TooltipProvider>
        </QueryClientWrapper>
    );

    return render(ui, {wrapper});
};

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

const makeProject = (
    overrides: Partial<AutomationWorkflowProjectType> & Pick<AutomationWorkflowProjectType, 'id' | 'name'>
): AutomationWorkflowProjectType => ({
    categoryId: null,
    description: null,
    lastPublishedVersion: null,
    published: false,
    tagIds: [],
    version: 1,
    workflowTemplates: [],
    ...overrides,
});

describe('AutomationWorkflowProjectList', () => {
    const onCreateWorkflow = vi.fn();
    const onDeleteProject = vi.fn();
    const onDeleteWorkflow = vi.fn();
    const onEditProject = vi.fn();
    const onImportWorkflow = vi.fn();
    const onPublishProject = vi.fn();
    const onSelectWorkflow = vi.fn();
    const onUpdateTags = vi.fn();

    const tags: EmbeddedTagType[] = [{id: '1', name: 'sales'}];

    const defaultProps = {
        onCreateWorkflow,
        onDeleteProject,
        onDeleteWorkflow,
        onEditProject,
        onImportWorkflow,
        onPublishProject,
        onSelectWorkflow,
        onUpdateTags,
        tags,
    };

    beforeEach(() => {
        onCreateWorkflow.mockReset();
        onDeleteProject.mockReset();
        onDeleteWorkflow.mockReset();
        onEditProject.mockReset();
        onImportWorkflow.mockReset();
        onPublishProject.mockReset();
        onSelectWorkflow.mockReset();
        onUpdateTags.mockReset();
    });

    it('renders project names and a draft badge', () => {
        const projects = [makeProject({id: 'p1', name: 'Alpha Project'})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('Alpha Project')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });

    it('renders a published badge with the published version', () => {
        const projects = [makeProject({id: 'p1', lastPublishedVersion: 3, name: 'Published Project', published: true})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('V3')).toBeInTheDocument();
        expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
    });

    it('renders the project tags as interactive chips', () => {
        const projects = [makeProject({id: 'p1', name: 'Tagged Project', tagIds: ['1']})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('sales')).toBeInTheDocument();
    });

    it('calls onUpdateTags with remaining tags when a tag chip is removed', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'p1', name: 'Tagged Project', tagIds: ['1']})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('sales')).toBeInTheDocument();

        // The remove button is a small icon button inside the tag chip — find by its container
        const tagChip = screen.getByText('sales').closest('div');

        const removeButton = tagChip!.querySelector('button');

        await user.click(removeButton!);

        expect(onUpdateTags).toHaveBeenCalledTimes(1);
        expect(onUpdateTags.mock.calls[0][0]).toMatchObject({id: 'p1'});
        expect(onUpdateTags.mock.calls[0][1]).toEqual([]);
    });

    it('calls onUpdateTags with new tag name when a tag is added via the + button', async () => {
        const user = userEvent.setup();

        const allTags: EmbeddedTagType[] = [
            {id: '1', name: 'sales'},
            {id: '2', name: 'finance'},
        ];

        const projects = [makeProject({id: 'p1', name: 'Tagged Project', tagIds: []})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} tags={allTags} />);

        // The + (add tag) button follows the "Tags:" label
        const tagsLabel = screen.getByText('Tags:');
        const tagListContainer = tagsLabel.closest('div')!;
        const addButton = tagListContainer.querySelector('button');

        await user.click(addButton!);

        const input = await screen.findByRole('combobox');

        await user.type(input, 'sales');

        const option = await screen.findByText('sales', {selector: '[class*="option"]'});

        await user.click(option);

        expect(onUpdateTags).toHaveBeenCalledTimes(1);
        expect(onUpdateTags.mock.calls[0][1]).toContain('sales');
    });

    it('calls onPublishProject when Publish is selected', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'project-7', name: 'My Project'})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-7-moreProjectActionsButton'));

        await user.click(await screen.findByRole('menuitem', {name: /publish/i}));

        expect(onPublishProject).toHaveBeenCalledWith('project-7');
    });

    it('calls onCreateWorkflow from the primary Workflow split-button', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'project-3', name: 'My Project'})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByRole('button', {name: /create workflow/i}));

        expect(onCreateWorkflow).toHaveBeenCalledWith('project-3');
    });

    it('calls onImportWorkflow from the split-button chevron dropdown', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'project-4', name: 'My Project'})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByRole('button', {name: /more workflow creation actions/i}));

        await user.click(await screen.findByRole('menuitem', {name: /import workflow/i}));

        expect(onImportWorkflow).toHaveBeenCalledWith('project-4');
    });

    it('expands a project to show its workflows and navigates on click', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                id: 'project-5',
                name: 'My Project',
                workflowTemplates: [
                    {
                        components: [],
                        description: null,
                        label: 'Sync Contacts',
                        lastModifiedDate: null,
                        triggers: [],
                        workflowUuid: 'wf-uuid-5',
                    },
                ],
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByText('1 workflow'));

        const workflowItem = await screen.findByText('Sync Contacts');

        await user.click(workflowItem);

        expect(onSelectWorkflow).toHaveBeenCalledWith('wf-uuid-5');
    });
});
