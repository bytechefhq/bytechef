import {TooltipProvider} from '@/components/ui/tooltip';
import {AutomationJourneyStageType} from '@/pages/automation/journey/getAutomationJourney';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AutomationGettingStartedQuery} from '@/shared/middleware/graphql';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, cleanup, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationGettingStarted from '../AutomationGettingStarted';

type StatusType = AutomationGettingStartedQuery['automationGettingStarted'];

const hoisted = vi.hoisted(() => ({
    dismissMutateMock: vi.fn(),
    useAutomationGettingStartedQueryMock: vi.fn(),
    useCreateProjectMutationMock: vi.fn(),
    useCreateProjectWorkflowMutationMock: vi.fn(),
    useDismissAutomationGettingStartedMutationMock: vi.fn(),
    useEnableProjectDeploymentMutationMock: vi.fn(),
    useGetProjectCategoriesQueryMock: vi.fn(),
    useGetProjectTagsQueryMock: vi.fn(),
    useGetWorkflowQueryMock: vi.fn(),
    useGetWorkspaceProjectsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationGettingStartedQuery: hoisted.useAutomationGettingStartedQueryMock,
    useDismissAutomationGettingStartedMutation: hoisted.useDismissAutomationGettingStartedMutationMock,
    useEnvironmentsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {project: (id: number) => ['project', id], projects: ['projects']},
    useGetWorkspaceProjectsQuery: hoisted.useGetWorkspaceProjectsQueryMock,
}));

vi.mock('@/shared/queries/automation/projectCategories.queries', () => ({
    ProjectCategoryKeys: {projectCategories: (id: number) => ['projectCategories', id]},
    useGetProjectCategoriesQuery: hoisted.useGetProjectCategoriesQueryMock,
}));

vi.mock('@/shared/queries/automation/projectTags.queries', () => ({
    ProjectTagKeys: {projectTags: ['projectTags']},
    useGetProjectTagsQuery: hoisted.useGetProjectTagsQueryMock,
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: hoisted.useGetWorkflowQueryMock,
}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    useCreateProjectMutation: hoisted.useCreateProjectMutationMock,
    useUpdateProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useCreateProjectWorkflowMutation: hoisted.useCreateProjectWorkflowMutationMock,
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useEnableProjectDeploymentMutation: hoisted.useEnableProjectDeploymentMutationMock,
}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionContentClipboardButton', () => ({
    default: ({value}: {value: string}) => <button data-testid="copy-button" data-value={value} type="button" />,
}));

const baseStatus: StatusType = {
    completed: false,
    deployed: false,
    firstRun: null,
    httpClientAdded: false,
    lastFailedRun: null,
    live: false,
    loggerAdded: false,
    projectCreated: false,
    published: false,
    workflowCreated: false,
};

const failedRun: NonNullable<StatusType['lastFailedRun']> = {
    date: '2026-11-11T10:00:00Z',
    errorMessage: 'Connection refused',
    jobId: '7',
    projectDeploymentEnabled: true,
    projectDeploymentId: '5',
    status: 'FAILED',
};

const firstRun: NonNullable<StatusType['firstRun']> = {
    date: '2026-11-11T10:00:00Z',
    errorMessage: null,
    jobId: '8',
    projectDeploymentEnabled: true,
    projectDeploymentId: '5',
    status: 'COMPLETED',
};

const STAGE_SIGNALS: Record<AutomationJourneyStageType, Partial<StatusType>> = {
    buildWorkflow: {projectCreated: true},
    createProject: {},
    deploy: {projectCreated: true, published: true, workflowCreated: true},
    enable: {deployed: true, projectCreated: true, published: true, workflowCreated: true},
    live: {deployed: true, live: true, projectCreated: true, published: true, workflowCreated: true},
    publish: {projectCreated: true, workflowCreated: true},
};

let queryClient: QueryClient;

const mockPage = (status: StatusType, stage: AutomationJourneyStageType = 'live') => {
    hoisted.useAutomationGettingStartedQueryMock.mockReturnValue({
        data: {automationGettingStarted: {...status, ...STAGE_SIGNALS[stage]}},
        error: null,
        isLoading: false,
    });
};

const rememberGuide = () => {
    localStorage.setItem(
        'bytechef.automation-getting-started.guide-workflow',
        JSON.stringify({projectId: 4, projectWorkflowId: 9})
    );

    hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 4, projectWorkflowIds: [9]}]});
};

const renderPage = () =>
    render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <MemoryRouter initialEntries={['/automation/getting-started']}>
                    <Routes>
                        <Route element={<AutomationGettingStarted />} path="/automation/getting-started" />

                        <Route element={<div>Projects page</div>} path="/automation/projects" />

                        <Route element={<div>Deployments page</div>} path="/automation/deployments" />

                        <Route
                            element={<div>Editor page</div>}
                            path="/automation/projects/:projectId/project-workflows/:projectWorkflowId"
                        />
                    </Routes>
                </MemoryRouter>
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('AutomationGettingStarted', () => {
    beforeEach(() => {
        queryClient = new QueryClient();

        vi.spyOn(queryClient, 'invalidateQueries');

        hoisted.dismissMutateMock.mockReset();
        hoisted.useAutomationGettingStartedQueryMock.mockReset();
        hoisted.useDismissAutomationGettingStartedMutationMock.mockReset();
        hoisted.useDismissAutomationGettingStartedMutationMock.mockReturnValue({
            isPending: false,
            mutate: hoisted.dismissMutateMock,
        });
        hoisted.useEnableProjectDeploymentMutationMock.mockReset();
        hoisted.useEnableProjectDeploymentMutationMock.mockReturnValue({
            isPending: false,
            mutate: vi.fn(),
        });
        hoisted.useGetWorkspaceProjectsQueryMock.mockReset();
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: []});
        hoisted.useCreateProjectMutationMock.mockReset();
        hoisted.useCreateProjectMutationMock.mockReturnValue({isPending: false, mutate: vi.fn()});
        hoisted.useCreateProjectWorkflowMutationMock.mockReset();
        hoisted.useCreateProjectWorkflowMutationMock.mockReturnValue({isPending: false, mutate: vi.fn()});
        hoisted.useGetProjectCategoriesQueryMock.mockReturnValue({data: [], error: null, isLoading: false});
        hoisted.useGetProjectTagsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});
        hoisted.useGetWorkflowQueryMock.mockReturnValue({data: undefined});

        localStorage.clear();

        mockPage(baseStatus, 'createProject');

        environmentStore.setState({currentEnvironmentId: 0});
        useWorkspaceStore.setState({currentWorkspaceId: 3});

        applicationInfoStore.setState((state) => ({
            application: {edition: EditionType.CE},
            docs: {...state.docs, communitySupportUrl: 'https://community.example.com'},
        }));
        authenticationStore.setState(authenticationStore.getInitialState(), true);
        permissionStore.setState({workspaceScopeStates: {}});
    });

    afterEach(() => {
        cleanup();
    });

    it('leaves for Deployments when the environment switches away from Development', async () => {
        renderPage();

        expect(screen.getByText('Getting Started')).toBeInTheDocument();

        act(() => {
            environmentStore.setState({currentEnvironmentId: 1});
        });

        expect(await screen.findByText('Deployments page')).toBeInTheDocument();
        expect(screen.queryByText('Getting Started')).not.toBeInTheDocument();
    });

    it('shows sub-steps for every step', () => {
        renderPage();

        expect(screen.getByText('Getting Started')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Build First Workflow'})).toBeInTheDocument();
        expect(screen.getByRole('list', {name: 'Project setup'})).toBeInTheDocument();
        expect(screen.getByRole('list', {name: 'Workflow setup'})).toBeInTheDocument();
        expect(screen.getByRole('list', {name: 'Publish setup'})).toBeInTheDocument();
        expect(screen.getByRole('list', {name: 'Deployment setup'})).toBeInTheDocument();
        expect(screen.getByText('Fetch a quote')).toBeInTheDocument();
        expect(screen.getByText('Deploy it in Development')).toBeInTheDocument();
        expect(hoisted.useAutomationGettingStartedQueryMock).toHaveBeenLastCalledWith(
            {workspaceId: '3'},
            expect.anything()
        );
    });

    it('shows the done texts of completed steps', () => {
        rememberGuide();
        mockPage(baseStatus, 'deploy');

        renderPage();

        expect(screen.getByText('Project created')).toBeInTheDocument();
        expect(screen.getByText('Workflow added')).toBeInTheDocument();
        expect(screen.getByText('Version published')).toBeInTheDocument();
    });

    it('keeps every step open while the guide has no project, whatever other projects reached', () => {
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 7, projectWorkflowIds: [3]}]});

        mockPage({...baseStatus, lastFailedRun: failedRun}, 'live');

        renderPage();

        expect(screen.queryByText('Project created')).not.toBeInTheDocument();
        expect(screen.queryByText('Workflow added')).not.toBeInTheDocument();
        expect(screen.queryByText('Version published')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create a project'})).toBeInTheDocument();
        expect(screen.queryByTestId('last-failed-run')).not.toBeInTheDocument();
    });

    it('ticks only the project step while the guide project has no guide workflow', () => {
        localStorage.setItem('bytechef.automation-getting-started.guide-project', '4');
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 4}, {id: 7, projectWorkflowIds: [3]}]});

        mockPage(baseStatus, 'enable');

        renderPage();

        expect(screen.getByText('Project created')).toBeInTheDocument();
        expect(screen.queryByText('Workflow added')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create a workflow'})).toBeInTheDocument();
    });

    it('copies the quote URL and the data pill', () => {
        renderPage();

        const copyValues = screen.getAllByTestId('copy-button').map((button) => button.getAttribute('data-value'));

        expect(copyValues).toEqual(['https://dummyjson.com/quotes/random', '${httpClient_1.quote}']);
    });

    it('waits for the first run', () => {
        rememberGuide();
        mockPage(baseStatus, 'live');

        renderPage();

        expect(screen.getByText('Waiting for the first run')).toBeInTheDocument();
        expect(screen.getByText("Click Run now in the editor's Getting started card.")).toBeInTheDocument();
        expect(screen.queryByTestId('run-received')).not.toBeInTheDocument();
    });

    it('shows no first run box before the deployment can run', () => {
        mockPage(baseStatus, 'enable');

        renderPage();

        expect(screen.queryByText('Waiting for the first run')).not.toBeInTheDocument();
    });

    it('shows the latest failed run', () => {
        rememberGuide();
        mockPage({...baseStatus, lastFailedRun: failedRun}, 'live');

        renderPage();

        const failedBox = screen.getByTestId('last-failed-run');

        expect(failedBox).toHaveTextContent('Run failed');
        expect(failedBox).toHaveTextContent('FAILED - Connection refused');
        expect(within(failedBox).getByRole('link', {name: 'View workflow executions'})).toHaveAttribute(
            'href',
            '/automation/workflow-executions'
        );
    });

    it('polls every 3 seconds only once a version is published and no run arrived', () => {
        renderPage();

        const publishedStatus = {...baseStatus, published: true};

        const getRefetchInterval = (status: StatusType) =>
            hoisted.useAutomationGettingStartedQueryMock.mock.lastCall?.[1].refetchInterval({
                state: {data: {automationGettingStarted: status}},
            });

        expect(getRefetchInterval(publishedStatus)).toBe(3000);
        expect(getRefetchInterval({...publishedStatus, lastFailedRun: failedRun})).toBe(3000);
        expect(getRefetchInterval({...publishedStatus, firstRun})).toBe(false);
        expect(getRefetchInterval({...publishedStatus, completed: true})).toBe(false);
        expect(getRefetchInterval(baseStatus)).toBe(false);
    });

    it('stops polling once the status query fails', () => {
        renderPage();

        const refetchInterval = hoisted.useAutomationGettingStartedQueryMock.mock.lastCall?.[1].refetchInterval;
        const publishedStatus = {...baseStatus, published: true};

        expect(refetchInterval({state: {data: {automationGettingStarted: publishedStatus}, error: null}})).toBe(3000);
        expect(
            refetchInterval({
                state: {data: {automationGettingStarted: publishedStatus}, error: new Error('Request failed')},
            })
        ).toBe(false);
    });

    it('redirects a viewer to projects without querying the status', async () => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        permissionStore.setState({
            workspaceScopeStates: {3: {0: {scopes: ['API_KEY_VIEW'], status: 'loaded'}}},
        });

        renderPage();

        expect(await screen.findByText('Projects page')).toBeInTheDocument();
        expect(hoisted.useAutomationGettingStartedQueryMock).toHaveBeenLastCalledWith(
            {workspaceId: '3'},
            expect.objectContaining({enabled: false})
        );
    });

    it('does not redirect or query the status while the scopes are loading', () => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        permissionStore.setState({workspaceScopeStates: {3: {0: {status: 'loading'}}}});

        renderPage();

        expect(screen.queryByText('Projects page')).not.toBeInTheDocument();
        expect(hoisted.useAutomationGettingStartedQueryMock).toHaveBeenLastCalledWith(
            {workspaceId: '3'},
            expect.objectContaining({enabled: false})
        );
    });

    it('queries the status for an editor', () => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        permissionStore.setState({
            workspaceScopeStates: {3: {0: {scopes: ['PROJECT_CREATE'], status: 'loaded'}}},
        });

        renderPage();

        expect(screen.queryByText('Projects page')).not.toBeInTheDocument();
        expect(hoisted.useAutomationGettingStartedQueryMock).toHaveBeenLastCalledWith(
            {workspaceId: '3'},
            expect.objectContaining({enabled: true})
        );
    });

    it('shows the first run without a Disable deployment button', () => {
        mockPage({...baseStatus, completed: true, firstRun}, 'live');

        renderPage();

        expect(screen.getByText("You're set up")).toBeInTheDocument();
        expect(screen.getByText('Run received')).toBeInTheDocument();
        expect(screen.getByText('Your workflow ran.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Dismiss'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Disable deployment'})).not.toBeInTheDocument();
        expect(screen.queryByText('Deployment disabled')).not.toBeInTheDocument();
    });

    it("shows success without disable when the run's deployment is gone", () => {
        mockPage(
            {...baseStatus, completed: true, firstRun: {...firstRun, projectDeploymentEnabled: null}},
            'createProject'
        );

        const {unmount} = renderPage();

        expect(screen.getByText('Run received')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Disable deployment'})).not.toBeInTheDocument();
        expect(screen.queryByText('Deployment disabled')).not.toBeInTheDocument();

        unmount();

        mockPage({...baseStatus, completed: true}, 'createProject');

        renderPage();

        expect(screen.getAllByText("You're set up")).toHaveLength(2);
        expect(screen.getByText('Setup is complete. See Workflow Executions for runs.')).toBeInTheDocument();
        expect(screen.queryByText('Run received')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Disable deployment'})).not.toBeInTheDocument();
        expect(screen.queryByText('Deployment disabled')).not.toBeInTheDocument();
    });

    it('shows an error instead of an empty checklist when the status query fails', () => {
        hoisted.useAutomationGettingStartedQueryMock.mockReturnValue({
            data: undefined,
            error: new Error('boom'),
            isLoading: false,
        });

        renderPage();

        expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        expect(screen.queryByRole('list', {name: 'Setup steps'})).not.toBeInTheDocument();
    });

    it('places the doc link inside the workflow step', () => {
        renderPage();

        const workflowList = screen.getByRole('list', {name: 'Workflow setup'});

        expect(workflowList.parentElement).toContainElement(screen.getByRole('link', {name: 'Build First Workflow'}));
    });

    it('hides Workflow Templates in next steps until there is a project', () => {
        renderPage();

        expect(screen.queryByRole('link', {name: 'Workflow Templates'})).not.toBeInTheDocument();
    });

    it("opens the guide project's workflow templates from next steps", () => {
        rememberGuide();

        renderPage();

        expect(screen.getByRole('link', {name: 'Workflow Templates'})).toHaveAttribute(
            'href',
            '/automation/projects/4/templates'
        );
    });

    it('renders next steps and the help card', () => {
        renderPage();

        expect(screen.getByRole('link', {name: 'Workflow Executions'})).toHaveAttribute(
            'href',
            '/automation/workflow-executions'
        );
        expect(screen.getByRole('link', {name: 'Star Repository on GitHub'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Build First Agent'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Configure Workflow Trigger'})).toBeInTheDocument();
        expect(
            screen
                .getAllByRole('link')
                .map((link) => link.textContent)
                .filter((text) =>
                    ['Build First Agent', 'Configure Workflow Trigger', 'Star Repository on GitHub'].some((title) =>
                        text?.includes(title)
                    )
                )
                .map((text) =>
                    ['Build First Agent', 'Configure Workflow Trigger', 'Star Repository on GitHub'].find((title) =>
                        text?.includes(title)
                    )
                )
        ).toEqual(['Configure Workflow Trigger', 'Build First Agent', 'Star Repository on GitHub']);
        expect(screen.getByText('Need help automating with ByteChef?')).toBeInTheDocument();
    });

    it('dismiss marks setup done and leaves the page', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'Dismiss'}));

        expect(hoisted.dismissMutateMock).not.toHaveBeenCalled();

        await userEvent.click(await screen.findByRole('button', {name: 'Dismiss Getting Started'}));

        expect(hoisted.dismissMutateMock).toHaveBeenCalledWith({workspaceId: '3'});

        const [mutationOptions] = hoisted.useDismissAutomationGettingStartedMutationMock.mock.lastCall ?? [];

        act(() => {
            mutationOptions.onSuccess();
        });

        await waitFor(() => {
            expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['automationGettingStartedCompleted'],
            });
        });

        expect(await screen.findByText('Projects page')).toBeInTheDocument();
    });

    it('invalidates the completed query on unmount after completion only', () => {
        const {unmount} = renderPage();

        unmount();

        expect(queryClient.invalidateQueries).not.toHaveBeenCalledWith({
            queryKey: ['automationGettingStartedCompleted'],
        });

        mockPage({...baseStatus, completed: true, firstRun}, 'live');

        renderPage().unmount();

        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
            queryKey: ['automationGettingStartedCompleted'],
        });
    });

    it('opens the create project dialog with the default name', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'Create a project'}));

        expect(await screen.findByText('Create Project')).toBeInTheDocument();
        expect(screen.getByLabelText('Name')).toHaveValue('Getting started');
    });

    it('opens the create workflow dialog for the guide project', async () => {
        localStorage.setItem('bytechef.automation-getting-started.guide-project', '4');
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 4, projectWorkflowIds: []}]});

        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'Create a workflow'}));

        expect(await screen.findByLabelText('Label')).toHaveValue('Learn ByteChef by doing');
    });

    it('offers no create workflow action without a project', () => {
        renderPage();

        expect(screen.queryByRole('button', {name: 'Create a workflow'})).not.toBeInTheDocument();
        expect(screen.queryByRole('link', {name: 'Create a workflow'})).not.toBeInTheDocument();
        expect(screen.getByText('Create a workflow')).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByText('Create a project first')).toBeInTheDocument();
    });

    it('asks for a guide project before a workflow even when other projects exist', () => {
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 4}, {id: 5}]});

        mockPage(baseStatus, 'buildWorkflow');

        renderPage();

        expect(screen.getByRole('button', {name: 'Create a project'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create a workflow'})).not.toBeInTheDocument();
        expect(screen.getByText('Create a project first')).toBeInTheDocument();
    });

    it('forgets a remembered guide project that no longer exists', () => {
        localStorage.setItem('bytechef.automation-getting-started.guide-project', '6');
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 4}]});

        renderPage();

        expect(screen.getByRole('button', {name: 'Create a project'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create a workflow'})).not.toBeInTheDocument();
    });

    it('links the later sub-steps to the remembered guide workflow', () => {
        localStorage.setItem(
            'bytechef.automation-getting-started.guide-workflow',
            JSON.stringify({projectId: 4, projectWorkflowId: 9})
        );
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 4, projectWorkflowIds: [9]}, {id: 5}]});

        renderPage();

        expect(screen.getByRole('link', {name: 'Fetch a quote'})).toHaveAttribute(
            'href',
            '/automation/projects/4/project-workflows/9'
        );
    });

    it('invalidates the status when a dialog closes', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'Create a project'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Cancel'}));

        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationGettingStarted']});
    });

    it('remembers the created workflow and opens it in the editor', async () => {
        localStorage.setItem('bytechef.automation-getting-started.guide-project', '4');
        hoisted.useGetWorkspaceProjectsQueryMock.mockReturnValue({data: [{id: 4, projectWorkflowIds: []}]});

        renderPage();

        const [mutationOptions] = hoisted.useCreateProjectWorkflowMutationMock.mock.lastCall ?? [];

        act(() => {
            mutationOptions.onSuccess({projectWorkflowId: 9});
        });

        expect(await screen.findByText('Editor page')).toBeInTheDocument();
        expect(localStorage.getItem('bytechef.automation-getting-started.guide-workflow')).toBe(
            JSON.stringify({projectId: 4, projectWorkflowId: 9})
        );
        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationGettingStarted']});
    });

    it('opens the workflow dialog for a project created in this visit', async () => {
        hoisted.useCreateProjectMutationMock.mockImplementation((options) => ({
            isPending: false,
            mutate: () => options.onSuccess(12),
        }));

        renderPage();

        expect(screen.queryByRole('button', {name: 'Create a workflow'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Create a project'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Save'}));

        expect(localStorage.getItem('bytechef.automation-getting-started.guide-project')).toBe('12');

        await userEvent.click(await screen.findByRole('button', {name: 'Create a workflow'}));

        expect(await screen.findByLabelText('Label')).toHaveValue('Learn ByteChef by doing');
    });
});
