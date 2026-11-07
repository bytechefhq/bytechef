import {TooltipProvider} from '@/components/ui/tooltip';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter, RouterProvider, createMemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowExecutions} from './WorkflowExecutions';
import useWorkflowExecutionSheetStore from './stores/useWorkflowExecutionSheetStore';

const hoisted = vi.hoisted(() => ({
    content: [] as object[],
    edition: 'CE',
    executionsRequests: [] as Array<{source?: string}>,
    navigate: vi.fn(),
    totalElements: 0,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/stores/useApplicationInfoStore')>()),
    useApplicationInfoStore: (selector: (state: {application: {edition: string}}) => unknown) =>
        selector({application: {edition: hoisted.edition}}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEnvironmentsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/projects.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projects.queries')>()),
    useGetWorkspaceProjectsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectDeployments.queries')>()),
    useGetProjectDeploymentQuery: () => ({data: undefined}),
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectWorkflows.queries')>()),
    useGetProjectVersionWorkflowsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/workflowExecutions.queries')>()),
    useGetWorkspaceProjectWorkflowExecutionsQuery: (request: {source?: string}) => {
        hoisted.executionsRequests.push(request);

        return {
            data: {
                content: hoisted.content,
                size: 20,
                totalElements: hoisted.totalElements,
                totalPages: Math.ceil(hoisted.totalElements / 20),
            },
            error: null,
            isFetching: false,
            isLoading: false,
            refetch: vi.fn(),
        };
    },
}));

vi.mock('./components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({default: () => null}));

const renderWorkflowExecutions = (initialEntries: string[] = ['/']) =>
    render(
        <MemoryRouter initialEntries={initialEntries}>
            <TooltipProvider>
                <WorkflowExecutions />
            </TooltipProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.content = [];
    hoisted.edition = 'CE';
    hoisted.executionsRequests = [];
    hoisted.totalElements = 0;

    useWorkflowExecutionSheetStore.setState({
        workflowExecutionId: 0,
        workflowExecutionKind: 'JOB',
        workflowExecutionSheetOpen: false,
    });
});

describe('WorkflowExecutions deep link', () => {
    it('opens the execution sheet for the id query param', () => {
        renderWorkflowExecutions(['/?id=42']);

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(42);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('JOB');
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
    });

    it('opens the execution sheet for a new id query param without remounting', async () => {
        const router = createMemoryRouter(
            [
                {
                    element: (
                        <TooltipProvider>
                            <WorkflowExecutions />
                        </TooltipProvider>
                    ),
                    path: '/',
                },
            ],
            {initialEntries: ['/?id=42']}
        );

        render(<RouterProvider router={router} />);

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(42);

        useWorkflowExecutionSheetStore.setState({workflowExecutionSheetOpen: false});

        await act(async () => {
            await router.navigate('/?id=43');
        });

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(43);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
    });

    it('ignores a non-numeric or zero id query param', () => {
        renderWorkflowExecutions(['/?id=abc']);

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);

        renderWorkflowExecutions(['/?id=0']);

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });
});

describe('WorkflowExecutions empty states', () => {
    it('guides a first run and hides the filters until there is something to filter', () => {
        renderWorkflowExecutions();

        expect(screen.getByRole('heading', {level: 2, name: 'No workflow runs yet'})).toBeInTheDocument();
        expect(screen.queryByText('Status')).not.toBeInTheDocument();
        expect(screen.getByText('Workflow Executions')).toBeInTheDocument();
    });

    it('keeps the filters when they match nothing and offers to clear them', async () => {
        renderWorkflowExecutions(['/?status=FAILED']);

        expect(screen.getByText('No Matching Executions')).toBeInTheDocument();
        expect(screen.getByText('Status')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Clear filters'}));

        expect(hoisted.navigate).toHaveBeenCalledWith(
            '/automation/workflow-executions?source=PROJECT&status=&startDate=&endDate=&projectId=&projectDeploymentId=&workflowId=&pageNumber='
        );
        expect(screen.getByRole('heading', {level: 2, name: 'No workflow runs yet'})).toBeInTheDocument();
    });

    it('guides a first run on a page number with nothing to page through', () => {
        renderWorkflowExecutions(['/?pageNumber=5']);

        expect(screen.getByRole('heading', {level: 2, name: 'No workflow runs yet'})).toBeInTheDocument();
        expect(screen.queryByText('No Matching Executions')).not.toBeInTheDocument();
    });

    it('offers the first page when the page number is past the last execution', async () => {
        hoisted.totalElements = 40;

        renderWorkflowExecutions(['/?pageNumber=5']);

        expect(screen.getByText('No Executions on This Page')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {level: 2, name: 'No workflow runs yet'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Go to first page'}));

        expect(hoisted.navigate).toHaveBeenCalledWith(
            '/automation/workflow-executions?source=PROJECT&status=&startDate=&endDate=&projectId=&projectDeploymentId=&workflowId=&pageNumber='
        );
    });
});

describe('WorkflowExecutions type', () => {
    beforeEach(() => {
        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
        Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
        Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
    });

    const mcpExecution = {
        id: 7,
        job: {id: '7', label: 'Report', status: 'COMPLETED'},
        project: {id: 3, name: 'Sales'},
        projectDeployment: {id: 5, name: 'Sales MCP', projectVersion: 1},
    };

    it('requests project executions and keeps the Deployment column and filter by default', () => {
        hoisted.content = [mcpExecution];

        renderWorkflowExecutions();

        expect(hoisted.executionsRequests.at(-1)?.source).toBe('PROJECT');
        expect(screen.getByRole('columnheader', {name: 'Deployment'})).toBeInTheDocument();
        expect(screen.getByText('Deployment', {selector: 'label'})).toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'Type'})).toHaveTextContent('Projects');
    });

    it('requests the executions of the source in the query and names the deployment column after it', () => {
        hoisted.content = [mcpExecution];

        renderWorkflowExecutions(['/?source=MCP_SERVER']);

        expect(hoisted.executionsRequests.at(-1)?.source).toBe('MCP_SERVER');
        expect(screen.getByRole('columnheader', {name: 'MCP server'})).toBeInTheDocument();
        expect(screen.queryByRole('columnheader', {name: 'Deployment'})).not.toBeInTheDocument();
        expect(screen.queryByText('Deployment', {selector: 'label'})).not.toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'Type'})).toHaveTextContent('MCP Servers');
    });

    it('falls back to project executions for an unknown source', () => {
        renderWorkflowExecutions(['/?source=UNKNOWN']);

        expect(hoisted.executionsRequests.at(-1)?.source).toBe('PROJECT');
    });

    it('keeps the filters visible for a source without executions and clears back to projects', async () => {
        renderWorkflowExecutions(['/?source=A2A_SERVER']);

        expect(screen.getByText('No Matching Executions')).toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'Type'})).toHaveTextContent('A2A Servers');

        await userEvent.click(screen.getByRole('button', {name: 'Clear filters'}));

        expect(hoisted.navigate).toHaveBeenCalledWith(
            '/automation/workflow-executions?source=PROJECT&status=&startDate=&endDate=&projectId=&projectDeploymentId=&workflowId=&pageNumber='
        );
        expect(hoisted.executionsRequests.at(-1)?.source).toBe('PROJECT');
    });

    it.each([
        ['CE', ['Projects', 'MCP Servers', 'A2A Servers']],
        ['EE', ['Projects', 'MCP Servers', 'A2A Servers', 'API Collections']],
    ])('offers the %s sources in the Type select', async (edition, expectedSources) => {
        hoisted.content = [mcpExecution];
        hoisted.edition = edition;

        renderWorkflowExecutions();

        await userEvent.click(screen.getByRole('combobox', {name: 'Type'}));

        expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual(expectedSources);
    });

    it('navigates to the selected source and drops the deployment filter', async () => {
        hoisted.content = [mcpExecution];

        renderWorkflowExecutions(['/?projectDeploymentId=5']);

        await userEvent.click(screen.getByRole('combobox', {name: 'Type'}));
        await userEvent.click(screen.getByRole('option', {name: 'MCP Servers'}));

        expect(hoisted.navigate).toHaveBeenLastCalledWith(
            '/automation/workflow-executions?source=MCP_SERVER&status=&startDate=&endDate=&projectId=&projectDeploymentId=&workflowId=&pageNumber='
        );
        expect(hoisted.executionsRequests.at(-1)).toMatchObject({projectDeploymentId: undefined, source: 'MCP_SERVER'});
    });
});
