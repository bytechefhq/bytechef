import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectWorkflowList from '@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowList';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const mocks = vi.hoisted(() => ({
    hasEnabledAiProvider: true,
    navigate: vi.fn(),
    workflows: [] as unknown[],
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}),
    };
});

vi.mock('react-router-dom', () => ({
    useNavigate: () => mocks.navigate,
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureProjectWorkflowCreated: vi.fn(),
        captureProjectWorkflowImported: vi.fn(),
    }),
}));

vi.mock('@/shared/hooks/useHasEnabledAiProvider', () => ({
    useHasEnabledAiProvider: () => ({hasEnabledAiProvider: mocks.hasEnabledAiProvider, isPending: false}),
}));

vi.mock('@/pages/automation/project/hooks/useConverterN8nToWorkflow', () => ({
    useConvertN8nToWorkflow: () => ({convertN8nWorkflow: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useCreateProjectWorkflowMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectWorkflowsQuery: () => ({data: mocks.workflows, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {
        project: (id: number) => ['projects', id],
        projects: ['projects'],
    },
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: ({triggerNode}: {triggerNode?: React.ReactNode}) =>
        triggerNode ? <div>{triggerNode}</div> : <div data-testid="workflow-dialog" />,
}));

vi.mock('@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowListItem', () => ({
    default: ({workflow}: {workflow: {label: string}}) => <li>{workflow.label}</li>,
}));

const mockProject = {id: 7, lastProjectVersion: 1, name: 'Test Project', workspaceId: 1};

const renderProjectWorkflowList = () =>
    render(
        <TooltipProvider>
            <ProjectWorkflowList
                componentDefinitions={[]}
                project={mockProject}
                queryEnabled
                taskDispatcherDefinitions={[]}
            />
        </TooltipProvider>
    );

const openMenu = async () => {
    renderProjectWorkflowList();

    await userEvent.click(document.querySelector('[aria-haspopup="menu"]')!);
};

describe('ProjectWorkflowList', () => {
    beforeEach(() => {
        windowResizeObserver();

        mocks.hasEnabledAiProvider = true;
        mocks.workflows = [];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should render the workflows when the project has some', () => {
        mocks.workflows = [{id: 'a', label: 'First workflow'}];

        renderProjectWorkflowList();

        expect(screen.getByText('First workflow')).toBeInTheDocument();
        expect(document.querySelector('[aria-haspopup="menu"]')).not.toBeInTheDocument();
    });

    it('should render with the design-system DropdownMenu', async () => {
        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'From Template'})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Import Workflow'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Import n8n Workflow'})).toBeInTheDocument();
    });

    it('should navigate to the templates when From Template is clicked', async () => {
        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'From Template'}));

        expect(mocks.navigate).toHaveBeenCalledWith('./7/templates');
    });

    it('should open the workflow file picker when Import Workflow is clicked', async () => {
        const clickSpy = vi.spyOn(HTMLInputElement.prototype, 'click');

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

        expect(clickSpy).toHaveBeenCalled();

        clickSpy.mockRestore();
    });

    it('should open the n8n file picker when Import n8n Workflow is clicked', async () => {
        const clickSpy = vi.spyOn(HTMLInputElement.prototype, 'click');

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Import n8n Workflow'}));

        expect(clickSpy).toHaveBeenCalled();

        clickSpy.mockRestore();
    });

    it('should disable Import n8n Workflow without an AI provider', async () => {
        mocks.hasEnabledAiProvider = false;

        await openMenu();

        expect(screen.getByRole('menuitem', {name: 'Import n8n Workflow'})).toHaveAttribute('aria-disabled', 'true');
    });
});
