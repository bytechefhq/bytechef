import {TooltipProvider} from '@/components/ui/tooltip';
import {AutomationProjectTagsQuery, AutomationProjectsQuery, CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {createTestQueryClientWrapper, screen, userEvent} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {ReactElement, ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationProjectList from '../components/automation-project-list/AutomationProjectList';

vi.mock('@/shared/queries/platform/environments.queries', () => ({
    useGetEnvironmentsQuery: () => ({data: [{id: 1, name: 'Development'}]}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationProjectConfigurationsQuery: () => ({
        data: {automationProjectConfigurations: []},
    }),
}));

vi.mock('../components/automation-project-configuration-dialog/AutomationProjectConfigurationDialog', () => ({
    default: ({onClose, onSuccess}: {onClose: () => void; onSuccess?: (configurationId: string) => void}) => (
        <button
            onClick={() => {
                onSuccess?.('configuration-9');

                onClose();
            }}
            type="button"
        >
            Save Configuration
        </button>
    ),
}));

const renderWithProviders = (ui: ReactElement, initialEntries = ['/embedded/automations']) => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    const wrapper = ({children}: {children: ReactNode}) => (
        <MemoryRouter initialEntries={initialEntries}>
            <QueryClientWrapper>
                <TooltipProvider>{children}</TooltipProvider>
            </QueryClientWrapper>
        </MemoryRouter>
    );

    return render(ui, {wrapper});
};

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type EmbeddedTagType = AutomationProjectTagsQuery['automationProjectTags'][number];

const makeProject = (
    overrides: Partial<AutomationProjectType> & Pick<AutomationProjectType, 'id' | 'name'>
): AutomationProjectType => ({
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

describe('AutomationProjectList', () => {
    const onCreateWorkflow = vi.fn();
    const onDeleteWorkflow = vi.fn();
    const onEditProject = vi.fn();
    const onEditProjectSource = vi.fn();
    const onEditWorkflow = vi.fn();
    const onImportWorkflow = vi.fn();
    const onSelectWorkflow = vi.fn();
    const onUpdateTags = vi.fn();

    const tags: EmbeddedTagType[] = [{id: '1', name: 'sales'}];

    const defaultProps = {
        onCreateWorkflow,
        onDeleteWorkflow,
        onEditProject,
        onEditProjectSource,
        onEditWorkflow,
        onImportWorkflow,
        onSelectWorkflow,
        onUpdateTags,
        tags,
    };

    beforeEach(() => {
        onCreateWorkflow.mockReset();
        onDeleteWorkflow.mockReset();
        onEditProject.mockReset();
        onEditProjectSource.mockReset();
        onEditWorkflow.mockReset();
        onImportWorkflow.mockReset();
        onSelectWorkflow.mockReset();
        onUpdateTags.mockReset();
    });

    it('renders project names and a draft badge', () => {
        const projects = [makeProject({id: 'p1', name: 'Alpha Project'})];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('Alpha Project')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });

    it('renders a published badge with the published version', () => {
        const projects = [makeProject({id: 'p1', lastPublishedVersion: 3, name: 'Published Project', published: true})];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('V3')).toBeInTheDocument();
        expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
    });

    it('renders the project tags as interactive chips', () => {
        const projects = [makeProject({id: 'p1', name: 'Tagged Project', tagIds: ['1']})];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('sales')).toBeInTheDocument();
    });

    it('calls onUpdateTags with remaining tags when a tag chip is removed', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'p1', name: 'Tagged Project', tagIds: ['1']})];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        expect(screen.getByText('sales')).toBeInTheDocument();

        // The remove button is a small icon button inside the tag chip — find by its container
        const tagChip = screen.getByText('sales').closest('div');

        const removeButton = tagChip!.querySelector('button');

        await user.click(removeButton!);

        expect(onUpdateTags).toHaveBeenCalledTimes(1);
        expect(onUpdateTags.mock.calls[0][0]).toMatchObject({id: 'p1'});
        expect(onUpdateTags.mock.calls[0][1]).toEqual([]);
    });

    it('calls onUpdateTags with new tag name when a tag is added via the Add tag button', async () => {
        const user = userEvent.setup();

        const allTags: EmbeddedTagType[] = [
            {id: '1', name: 'sales'},
            {id: '2', name: 'finance'},
        ];

        const projects = [makeProject({id: 'p1', name: 'Tagged Project', tagIds: []})];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} tags={allTags} />);

        await user.click(screen.getByRole('button', {name: 'Add tag'}));

        const input = await screen.findByRole('combobox');

        await user.type(input, 'sales');

        const option = await screen.findByText('sales', {selector: '[class*="option"]'});

        await user.click(option);

        expect(onUpdateTags).toHaveBeenCalledTimes(1);
        expect(onUpdateTags.mock.calls[0][1]).toContain('sales');
    });

    it('opens the publish dialog when Publish is selected', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                id: 'project-7',
                name: 'My Project',
                workflowTemplates: [
                    {
                        components: [],
                        description: null,
                        label: 'Sync',
                        lastModifiedDate: null,
                        permissionExpression: null,
                        triggers: [],
                        workflowId: 'draft-wf-7',
                        workflowUuid: 'wf-uuid-7',
                    },
                ],
            }),
        ];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-7-moreProjectActionsButton'));

        await user.click(await screen.findByRole('menuitem', {name: /publish/i}));

        expect(await screen.findByRole('dialog', {name: 'Publish Project My Project'})).toBeInTheDocument();
        expect(screen.getByLabelText('Description')).toBeInTheDocument();
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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-10-moreProjectActionsButton'));

        await screen.findByRole('menuitem', {name: /view references/i});

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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByTestId('project-12-moreProjectActionsButton'));

        expect(await screen.findByRole('menuitem', {name: /publish/i})).not.toHaveAttribute('data-disabled');
    });

    it('does not show Edit Source for a visual project', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'project-8', name: 'Visual Project'})];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByText('1 workflow'));

        await user.click(await screen.findByRole('button', {name: /more workflow creation actions/i}));

        await user.click(await screen.findByRole('menuitem', {name: /import workflow/i}));

        expect(onImportWorkflow).toHaveBeenCalledWith('project-4');
    });

    it('imports a workflow from the empty Workflows tab of a project', async () => {
        const user = userEvent.setup();

        const projects = [makeProject({id: 'project-8', name: 'Empty Project'})];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByText('0 workflows'));

        expect(await screen.findByText('No Workflows')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'More Workflow Creation Actions'}));

        await user.click(await screen.findByRole('menuitem', {name: 'Import Workflow'}));

        expect(onImportWorkflow).toHaveBeenCalledWith('project-8');
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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        const [draftTrigger, liveTrigger] = screen.getAllByText('0 workflows');

        await user.click(draftTrigger);
        await user.click(screen.getByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByText('Publish the project to configure it')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();

        await user.click(liveTrigger);

        const liveTabs = screen.getAllByRole('tab', {name: 'Configurations'});

        await user.click(liveTabs[liveTabs.length - 1]);

        expect(await screen.findByRole('button', {name: 'Create Configuration'})).toBeEnabled();
    });

    it('opens the publish dialog from Publish on the Configurations tab of a draft project', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                id: 'draft',
                name: 'Draft Project',
                workflowTemplates: [
                    {
                        components: [],
                        description: null,
                        label: 'Sync',
                        lastModifiedDate: null,
                        permissionExpression: null,
                        triggers: [],
                        workflowId: 'draft-wf-1',
                        workflowUuid: 'wf-uuid-1',
                    },
                ],
            }),
        ];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />, [
            '/embedded/automations?projectId=draft&tab=configurations',
        ]);

        await user.click(await screen.findByRole('button', {name: 'Publish'}));

        expect(await screen.findByRole('dialog', {name: 'Publish Project Draft Project'})).toBeInTheDocument();
    });

    it('opens the project on its Configurations tab from the projectId and tab parameters', () => {
        const projects = [
            makeProject({id: 'other', name: 'Other Project'}),
            makeProject({
                configurationCount: 1,
                id: 'live',
                lastPublishedVersion: 1,
                name: 'Live Project',
                published: true,
            }),
        ];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />, [
            '/embedded/automations?projectId=live&tab=configurations',
        ]);

        const configurationsTabs = screen.getAllByRole('tab', {name: 'Configurations'});

        expect(configurationsTabs).toHaveLength(1);
        expect(configurationsTabs[0]).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('button', {name: 'Create Configuration'})).toHaveTextContent('New Configuration');
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

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        await user.click(screen.getByText('1 workflow'));

        const workflowItem = await screen.findByText('Sync Contacts');

        await user.click(workflowItem);

        expect(onSelectWorkflow).toHaveBeenCalledWith('wf-uuid-5');
    });

    it('opens the project on its Configurations tab after a configuration is created from the project menu', async () => {
        const user = userEvent.setup();

        const projects = [
            makeProject({
                configurationCount: 0,
                id: 'live',
                lastPublishedVersion: 1,
                name: 'Live Project',
                published: true,
            }),
        ];

        renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

        expect(screen.queryByRole('tab', {name: 'Configurations'})).not.toBeInTheDocument();

        await user.click(screen.getByTestId('live-moreProjectActionsButton'));

        await user.click(await screen.findByRole('menuitem', {name: 'New Configuration'}));

        await user.click(await screen.findByRole('button', {name: 'Save Configuration'}));

        expect(await screen.findByRole('tab', {name: 'Configurations'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.queryByRole('button', {name: 'Save Configuration'})).not.toBeInTheDocument();
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

            renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

            await user.click(screen.getByText('1 workflow'));

            await screen.findByText(workflowTemplate.label);

            expect(screen.queryByRole('button', {name: /create workflow/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /more workflow creation actions/i})).not.toBeInTheDocument();

            await user.click(screen.getByTestId('project-10-moreProjectActionsButton'));

            await screen.findByRole('menuitem', {name: /edit source/i});

            expect(screen.getByRole('menuitem', {name: /view references/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /publish/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /edit project/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /delete project/i})).toBeInTheDocument();
            expect(screen.queryByRole('menuitem', {name: /new workflow/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('menuitem', {name: /import workflow/i})).not.toBeInTheDocument();
        });

        it('does not offer Delete Workflow on the workflow rows of a code project', async () => {
            const user = userEvent.setup();

            const projects = [
                makeProject({
                    codeWorkflowProject: true,
                    id: 'project-11',
                    name: 'Code Project',
                    workflowTemplates: [workflowTemplate],
                }),
            ];

            renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

            await user.click(screen.getByText('1 workflow'));
            await user.click(await screen.findByRole('button', {name: 'Workflow Actions'}));

            await screen.findByRole('menuitem', {name: 'Edit Workflow'});

            expect(screen.queryByRole('menuitem', {name: 'Delete Workflow'})).not.toBeInTheDocument();
        });

        it('still offers workflow creation and import on a visual project', async () => {
            const user = userEvent.setup();

            const projects = [
                makeProject({id: 'project-12', name: 'Visual Project', workflowTemplates: [workflowTemplate]}),
            ];

            renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

            await user.click(screen.getByText('1 workflow'));

            expect(await screen.findByRole('button', {name: /create workflow/i})).toBeInTheDocument();

            await user.click(screen.getByTestId('project-12-moreProjectActionsButton'));

            expect(await screen.findByRole('menuitem', {name: /new workflow/i})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /import workflow/i})).toBeInTheDocument();
        });

        it('still offers Delete Workflow on the workflow rows of a visual project', async () => {
            const user = userEvent.setup();

            const projects = [
                makeProject({id: 'project-13', name: 'Visual Project', workflowTemplates: [workflowTemplate]}),
            ];

            renderWithProviders(<AutomationProjectList {...defaultProps} projects={projects} />);

            await user.click(screen.getByText('1 workflow'));
            await user.click(await screen.findByRole('button', {name: 'Workflow actions for Sync Contacts'}));

            expect(await screen.findByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
        });
    });
});
