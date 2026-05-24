import {TooltipProvider} from '@/components/ui/tooltip';
import {createTestQueryClientWrapper, screen, userEvent} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {ReactElement, ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflows from '../AutomationWorkflows';

const renderWithProviders = (ui: ReactElement) => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientWrapper>
            <TooltipProvider>{children}</TooltipProvider>
        </QueryClientWrapper>
    );

    return render(ui, {wrapper});
};

// ---------------------------------------------------------------------------
// Hoisted mocks
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => {
    return {
        categories: [] as Array<{id: string; name: string}>,
        createProjectMutate: vi.fn(),
        createWorkflowMutate: vi.fn(),
        deleteProjectMutate: vi.fn(),
        deleteWorkflowMutate: vi.fn(),
        navigateMock: vi.fn(),
        projects: [] as Array<{
            categoryId: string | null;
            description: string | null;
            id: string;
            lastPublishedVersion: number | null;
            name: string;
            published: boolean;
            tagIds: Array<string>;
            workflowTemplates: Array<{description: string | null; label: string | null; workflowUuid: string}>;
        }>,
        publishProjectMutate: vi.fn(),
        tags: [] as Array<{id: string; name: string}>,
        updateProjectMutate: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationWorkflowProjectCategoriesQuery: () => ({
        data: {automationWorkflowProjectCategories: hoisted.categories},
        error: null,
        isLoading: false,
    }),
    useAutomationWorkflowProjectTagsQuery: () => ({
        data: {automationWorkflowProjectTags: hoisted.tags},
        error: null,
        isLoading: false,
    }),
    useAutomationWorkflowProjectsQuery: () => ({
        data: {automationWorkflowProjects: hoisted.projects},
        error: null,
        isLoading: false,
    }),
    useCreateAutomationWorkflowProjectMutation: () => ({
        isPending: false,
        mutate: hoisted.createProjectMutate,
    }),
    useCreateAutomationWorkflowProjectWorkflowMutation: () => ({
        isPending: false,
        mutate: hoisted.createWorkflowMutate,
    }),
    useDeleteAutomationWorkflowProjectMutation: () => ({
        isPending: false,
        mutate: hoisted.deleteProjectMutate,
    }),
    useDeleteAutomationWorkflowProjectWorkflowMutation: () => ({
        isPending: false,
        mutate: hoisted.deleteWorkflowMutate,
    }),
    usePublishAutomationWorkflowProjectMutation: () => ({
        isPending: false,
        mutate: hoisted.publishProjectMutate,
    }),
    useUpdateAutomationWorkflowProjectMutation: () => ({
        isPending: false,
        mutate: hoisted.updateProjectMutate,
    }),
}));

vi.mock('react-router-dom', () => ({
    Link: ({children}: {children: React.ReactNode}) => <a>{children}</a>,
    useNavigate: () => hoisted.navigateMock,
    useSearchParams: () => [new URLSearchParams(), vi.fn()],
}));

vi.mock(import('@tanstack/react-query'), async (importOriginal) => {
    const actual = await importOriginal();

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}) as unknown as ReturnType<typeof actual.useQueryClient>,
    };
});

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

beforeEach(() => {
    hoisted.categories = [];
    hoisted.createProjectMutate.mockReset();
    hoisted.createWorkflowMutate.mockReset();
    hoisted.deleteProjectMutate.mockReset();
    hoisted.deleteWorkflowMutate.mockReset();
    hoisted.navigateMock.mockReset();
    hoisted.projects = [];
    hoisted.publishProjectMutate.mockReset();
    hoisted.tags = [];
    hoisted.updateProjectMutate.mockReset();
});

describe('AutomationWorkflows', () => {
    it('renders the empty state when there are no projects', () => {
        hoisted.projects = [];

        renderWithProviders(<AutomationWorkflows />);

        expect(screen.getByText('No Projects')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /create project/i})).toBeInTheDocument();
    });

    it('renders the project list from the query', () => {
        hoisted.projects = [
            {
                categoryId: null,
                description: 'CRM project',
                id: 'project-1',
                lastPublishedVersion: null,
                name: 'CRM Project',
                published: false,
                tagIds: [],
                workflowTemplates: [{description: null, label: 'Sync Contacts', workflowUuid: 'wf-1'}],
            },
        ];

        renderWithProviders(<AutomationWorkflows />);

        expect(screen.getByText('CRM Project')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });

    it('renders the Categories and Tags filter sidebar sections', () => {
        hoisted.categories = [{id: '1', name: 'CRM'}];
        hoisted.tags = [{id: '2', name: 'lead'}];
        hoisted.projects = [
            {
                categoryId: '1',
                description: null,
                id: 'project-1',
                lastPublishedVersion: null,
                name: 'CRM Project',
                published: false,
                tagIds: ['2'],
                workflowTemplates: [],
            },
        ];

        renderWithProviders(<AutomationWorkflows />);

        expect(screen.getByText('Categories')).toBeInTheDocument();
        expect(screen.getByText('Tags')).toBeInTheDocument();
        expect(screen.getByText('CRM')).toBeInTheDocument();
        expect(screen.getAllByText('lead').length).toBeGreaterThan(0);
    });

    it('shows only categories returned by the embedded categories query in the filter sidebar', () => {
        hoisted.categories = [{id: '1', name: 'Used'}];
        hoisted.projects = [
            {
                categoryId: '1',
                description: null,
                id: 'project-1',
                lastPublishedVersion: null,
                name: 'Used Project',
                published: false,
                tagIds: [],
                workflowTemplates: [],
            },
        ];

        renderWithProviders(<AutomationWorkflows />);

        expect(screen.getByText('Used')).toBeInTheDocument();
        expect(screen.queryByText('Unused')).toBeNull();
    });

    it('hides the header but keeps the left sidebar when there are no projects', () => {
        hoisted.projects = [];

        renderWithProviders(<AutomationWorkflows />);

        expect(screen.getByText('No Projects')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^new project$/i})).toBeNull();
        expect(screen.getByText('Categories')).toBeInTheDocument();
    });

    it('opens the project dialog when New Project is clicked', async () => {
        const user = userEvent.setup();

        renderWithProviders(<AutomationWorkflows />);

        await user.click(screen.getByRole('button', {name: /create project/i}));

        expect(await screen.findByPlaceholderText('My CRM Project')).toBeInTheDocument();
        expect(screen.getByText('Use this to create a project which will contain workflows')).toBeInTheDocument();
    });

    it('calls the create project mutation when the dialog is submitted', async () => {
        const user = userEvent.setup();

        renderWithProviders(<AutomationWorkflows />);

        await user.click(screen.getByRole('button', {name: /create project/i}));

        const nameInput = await screen.findByPlaceholderText('My CRM Project');

        await user.type(nameInput, 'New Project');

        await user.click(screen.getByRole('button', {name: /^save$/i}));

        expect(hoisted.createProjectMutate).toHaveBeenCalledTimes(1);
        expect(hoisted.createProjectMutate.mock.calls[0][0]).toMatchObject({name: 'New Project'});
    });

    it('calls the update mutation with tag names when a tag is removed from the inline tag list', async () => {
        const user = userEvent.setup();

        hoisted.tags = [{id: '1', name: 'sales'}];
        hoisted.projects = [
            {
                categoryId: null,
                description: null,
                id: 'project-tag-1',
                lastPublishedVersion: null,
                name: 'Tagged Project',
                published: false,
                tagIds: ['1'],
                workflowTemplates: [],
            },
        ];

        renderWithProviders(<AutomationWorkflows />);

        // The tag chip renders "sales" in a <span class="py-1"> inside the TagList
        const tagChipSpans = screen.getAllByText('sales');
        const tagChipSpan = tagChipSpans.find((element) => element.tagName === 'SPAN' && element.className === 'py-1');

        expect(tagChipSpan).toBeDefined();

        const tagChip = tagChipSpan!.closest('div');
        const removeButton = tagChip!.querySelector('button');

        await user.click(removeButton!);

        expect(hoisted.updateProjectMutate).toHaveBeenCalledTimes(1);

        const mutateArg = hoisted.updateProjectMutate.mock.calls[0][0];

        expect(mutateArg).toMatchObject({
            category: undefined,
            description: undefined,
            id: 'project-tag-1',
            name: 'Tagged Project',
            tags: [],
        });
    });

    it('calls the publish mutation when Publish is selected from the project menu', async () => {
        const user = userEvent.setup();

        hoisted.projects = [
            {
                categoryId: null,
                description: null,
                id: 'project-9',
                lastPublishedVersion: null,
                name: 'Publishable Project',
                published: false,
                tagIds: [],
                workflowTemplates: [],
            },
        ];

        renderWithProviders(<AutomationWorkflows />);

        await user.click(screen.getByTestId('project-9-moreProjectActionsButton'));

        await user.click(await screen.findByRole('menuitem', {name: /publish/i}));

        expect(hoisted.publishProjectMutate).toHaveBeenCalledTimes(1);
        expect(hoisted.publishProjectMutate.mock.calls[0][0]).toMatchObject({id: 'project-9'});
    });

    it('opens the Create Workflow dialog when + Workflow is clicked', async () => {
        const user = userEvent.setup();

        hoisted.projects = [
            {
                categoryId: null,
                description: null,
                id: 'project-10',
                lastPublishedVersion: null,
                name: 'My Project',
                published: false,
                tagIds: [],
                workflowTemplates: [],
            },
        ];

        renderWithProviders(<AutomationWorkflows />);

        await user.click(screen.getByRole('button', {name: /create workflow/i}));

        expect(await screen.findByText('Create Workflow')).toBeInTheDocument();
        expect(screen.getByText('Create a new workflow by filling out the form below.')).toBeInTheDocument();
    });

    it('calls the create workflow mutation with label and definition on dialog save', async () => {
        const user = userEvent.setup();

        hoisted.projects = [
            {
                categoryId: null,
                description: null,
                id: 'project-11',
                lastPublishedVersion: null,
                name: 'My Project',
                published: false,
                tagIds: [],
                workflowTemplates: [],
            },
        ];

        renderWithProviders(<AutomationWorkflows />);

        await user.click(screen.getByRole('button', {name: /create workflow/i}));

        const labelInput = await screen.findByRole('textbox', {name: /label/i});

        await user.type(labelInput, 'My New Workflow');

        await user.click(screen.getByRole('button', {name: /^save$/i}));

        expect(hoisted.createWorkflowMutate).toHaveBeenCalledTimes(1);

        const {definition, projectId} = hoisted.createWorkflowMutate.mock.calls[0][0];

        expect(projectId).toBe('project-11');

        const parsed = JSON.parse(definition);

        expect(parsed.label).toBe('My New Workflow');
    });
});
