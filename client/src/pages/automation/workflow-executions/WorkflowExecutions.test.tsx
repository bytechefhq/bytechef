import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowExecutions} from './WorkflowExecutions';

const hoisted = vi.hoisted(() => ({navigate: vi.fn(), totalElements: 0}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {application: {edition: string}}) => unknown) =>
        selector({application: {edition: 'CE'}}),
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
    useGetWorkspaceProjectWorkflowExecutionsQuery: () => ({
        data: {
            content: [],
            size: 20,
            totalElements: hoisted.totalElements,
            totalPages: Math.ceil(hoisted.totalElements / 20),
        },
        error: null,
        isFetching: false,
        isLoading: false,
        refetch: vi.fn(),
    }),
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

    hoisted.totalElements = 0;
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
            '/automation/executions?status=&startDate=&endDate=&projectId=&projectDeploymentId=&workflowId=&pageNumber='
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
            '/automation/executions?status=&startDate=&endDate=&projectId=&projectDeploymentId=&workflowId=&pageNumber='
        );
    });
});
