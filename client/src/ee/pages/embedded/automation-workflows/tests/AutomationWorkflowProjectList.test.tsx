import {TooltipProvider} from '@/components/ui/tooltip';
import {
    AutomationWorkflowProjectTagsQuery,
    AutomationWorkflowProjectsQuery,
    CodeWorkflowLanguage,
} from '@/shared/middleware/graphql';
import {createTestQueryClientWrapper, screen, userEvent} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {ReactElement, ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectList from '../components/automation-workflow-project-list/AutomationWorkflowProjectList';

vi.mock('@/shared/queries/platform/environments.queries', () => ({
    useGetEnvironmentsQuery: () => ({data: [{id: 1, name: 'Development'}]}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: []},
    }),
}));

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
    codeWorkflowDraftPublishable: null,
    codeWorkflowLanguage: null,
    codeWorkflowProject: false,
    configurationCount: 0,
    description: null,
    lastPublishedDate: null,
    lastPublishedVersion: null,
    permissionExpression: null,
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
    const onEditProjectSource = vi.fn();
    const onEditWorkflow = vi.fn();
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
        onEditProjectSource,
        onEditWorkflow,
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
        onEditProjectSource.mockReset();
        onEditWorkflow.mockReset();
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

    it('renders the code language badge for a code project', () => {
        const projects = [
            makeProject({
                codeWorkflowLanguage: CodeWorkflowLanguage.Javascript,
                codeWorkflowProject: true,
                id: 'p1',
                name: 'Code Project',
            }),
            makeProject({id: 'p2', name: 'Visual Project'}),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        expect(screen.getAllByText('JavaScript')).toHaveLength(1);
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

    it('calls onEditProjectSource from the Edit Source item of a code project', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                codeWorkflowLanguage: CodeWorkflowLanguage.Javascript,
                codeWorkflowProject: true,
                id: 'project-9',
                name: 'Code Project',
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-9-moreProjectActionsButton'));

        await user.click(await screen.findByRole('menuitem', {name: /edit source/i}));

        expect(onEditProjectSource).toHaveBeenCalledWith('project-9');
    });

    it('does not show Edit Source for a Java code project', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                codeWorkflowLanguage: CodeWorkflowLanguage.Java,
                codeWorkflowProject: true,
                id: 'project-10',
                name: 'Java Project',
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-10-moreProjectActionsButton'));

        await screen.findByRole('menuitem', {name: 'Usage'});

        expect(screen.queryByRole('menuitem', {name: /edit source/i})).not.toBeInTheDocument();
    });

    it('disables Publish for a code project whose draft has no source of its own', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                codeWorkflowDraftPublishable: false,
                codeWorkflowLanguage: CodeWorkflowLanguage.Python,
                codeWorkflowProject: true,
                id: 'project-11',
                name: 'Unpublishable Project',
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-11-moreProjectActionsButton'));

        expect(await screen.findByRole('menuitem', {name: /publish/i})).toHaveAttribute('data-disabled');
    });

    it('enables Publish for a code project whose draft is publishable', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                codeWorkflowDraftPublishable: true,
                codeWorkflowLanguage: CodeWorkflowLanguage.Python,
                codeWorkflowProject: true,
                id: 'project-12',
                name: 'Publishable Project',
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-12-moreProjectActionsButton'));

        expect(await screen.findByRole('menuitem', {name: /publish/i})).not.toHaveAttribute('data-disabled');
    });

    it('does not show Edit Source for a visual project', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'project-8', name: 'Visual Project'})];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-8-moreProjectActionsButton'));

        await screen.findByRole('menuitem', {name: /publish/i});

        expect(screen.queryByRole('menuitem', {name: /edit source/i})).not.toBeInTheDocument();
    });

    it('calls onCreateWorkflow from the New Workflow button next to the Workflows heading', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                id: 'project-3',
                name: 'My Project',
                workflowTemplates: [
                    {
                        components: [],
                        description: null,
                        label: 'Sync Contacts',
                        lastModifiedDate: null,
                        permissionExpression: null,
                        triggers: [],
                        workflowId: 'draft-wf-uuid-3',
                        workflowUuid: 'wf-uuid-3',
                    },
                ],
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByText('1 workflow'));

        await user.click(await screen.findByRole('button', {name: /create workflow/i}));

        expect(onCreateWorkflow).toHaveBeenCalledWith('project-3');
    });

    it('calls onImportWorkflow from the New Workflow chevron dropdown', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                id: 'project-4',
                name: 'My Project',
                workflowTemplates: [
                    {
                        components: [],
                        description: null,
                        label: 'Sync Contacts',
                        lastModifiedDate: null,
                        permissionExpression: null,
                        triggers: [],
                        workflowId: 'draft-wf-uuid-4',
                        workflowUuid: 'wf-uuid-4',
                    },
                ],
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByText('1 workflow'));

        await user.click(await screen.findByRole('button', {name: /more workflow creation actions/i}));

        await user.click(await screen.findByRole('menuitem', {name: /import workflow/i}));

        expect(onImportWorkflow).toHaveBeenCalledWith('project-4');
    });

    it('offers configuration creation only once the project is published and has configurations', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({id: 'draft', name: 'Draft Project'}),
            makeProject({
                configurationCount: 1,
                id: 'live',
                lastPublishedVersion: 1,
                name: 'Live Project',
                published: true,
            }),
        ];

        renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

        const [draftTrigger, liveTrigger] = screen.getAllByText('0 workflows');

        await user.click(draftTrigger);
        await user.click(screen.getByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByText('Publish the project to configure it')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Configuration'})).not.toBeInTheDocument();

        await user.click(liveTrigger);

        const liveTabs = screen.getAllByRole('tab', {name: 'Configurations'});

        await user.click(liveTabs[liveTabs.length - 1]);

        expect(await screen.findByRole('button', {name: 'New Configuration'})).toBeEnabled();
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
                        permissionExpression: null,
                        triggers: [],
                        workflowId: 'draft-wf-uuid-5',
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

    describe('code projects', () => {
        const workflowTemplate = {
            components: [],
            description: null,
            label: 'Sync Contacts',
            lastModifiedDate: null,
            permissionExpression: null,
            triggers: [],
            workflowId: 'draft-wf-uuid-6',
            workflowUuid: 'wf-uuid-6',
        };

        it('does not offer workflow creation or import on a code project', async () => {
            const user = userEvent.setup();

            const projects = [
                makeProject({
                    codeWorkflowLanguage: CodeWorkflowLanguage.Javascript,
                    codeWorkflowProject: true,
                    id: 'project-10',
                    name: 'Code Project',
                    workflowTemplates: [workflowTemplate],
                }),
            ];

            renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

            expect(screen.queryByRole('button', {name: /create workflow/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /more workflow creation actions/i})).not.toBeInTheDocument();

            await user.click(screen.getByTestId('project-10-moreProjectActionsButton'));

            await screen.findByRole('menuitem', {name: /edit source/i});

            expect(screen.getByRole('menuitem', {name: 'Usage'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /publish/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /edit project/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /delete project/i})).toBeInTheDocument();
            expect(screen.queryByRole('menuitem', {name: /new workflow/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('menuitem', {name: /import workflow/i})).not.toBeInTheDocument();
        });

        it('shows no workflow actions menu on the workflow rows of a code project', async () => {
            const user = userEvent.setup();

            const projects = [
                makeProject({
                    codeWorkflowProject: true,
                    id: 'project-11',
                    name: 'Code Project',
                    workflowTemplates: [workflowTemplate],
                }),
            ];

            renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

            await user.click(screen.getByText('1 workflow'));

            await screen.findByText(workflowTemplate.label);

            expect(screen.queryByRole('button', {name: 'Workflow Actions'})).not.toBeInTheDocument();
        });

        it('still offers workflow creation and import on a visual project', async () => {
            const user = userEvent.setup();

            const projects = [
                makeProject({id: 'project-12', name: 'Visual Project', workflowTemplates: [workflowTemplate]}),
            ];

            renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

            expect(screen.getByRole('button', {name: /create workflow/i})).toBeInTheDocument();

            await user.click(screen.getByTestId('project-12-moreProjectActionsButton'));

            expect(await screen.findByRole('menuitem', {name: /new workflow/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /import workflow/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'Usage'})).toBeInTheDocument();
        });

        it('still offers Delete Workflow on the workflow rows of a visual project', async () => {
            const user = userEvent.setup();

            const projects = [
                makeProject({id: 'project-13', name: 'Visual Project', workflowTemplates: [workflowTemplate]}),
            ];

            renderWithProviders(<AutomationWorkflowProjectList {...defaultProps} projects={projects} />);

            await user.click(screen.getByText('1 workflow'));
            await user.click(await screen.findByRole('button', {name: 'Workflow Actions'}));

            expect(await screen.findByRole('menuitem', {name: 'Delete Workflow'})).toBeInTheDocument();
        });
    });
});
