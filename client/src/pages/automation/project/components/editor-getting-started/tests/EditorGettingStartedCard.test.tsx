import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import EditorGettingStartedCard from '../EditorGettingStartedCard';

const mocks = vi.hoisted(() => ({
    actions: {
        enable: vi.fn(),
        enablePending: false,
        openDeploy: vi.fn(),
        openExecutions: vi.fn(),
        openPublish: vi.fn(),
        runNow: vi.fn(),
        runPending: false,
        runWaiting: false,
    },
    canCreateProject: true,
    deployments: [] as unknown[],
    fetchedAfterMount: true,
    invalidateQueries: vi.fn(),
    nodeOutput: undefined as unknown,
    nodeOutputQuery: vi.fn(),
    projects: [{id: 5, projectWorkflowIds: [7]}] as unknown[],
    status: {completed: false} as Record<string, unknown>,
    statusQuery: vi.fn(),
    tasks: [] as {name?: string; parameters?: Record<string, unknown>; type: string}[],
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: unknown) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: unknown) => unknown) => selector({currentEnvironmentId: 1}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: (state: unknown) => unknown) => selector({workflow: {id: 'workflow-1', tasks: mocks.tasks}}),
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => mocks.canCreateProject,
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: mocks.projects}),
}));

vi.mock('@/shared/queries/platform/workflowNodeOutputs.queries', () => ({
    useGetWorkflowNodeOutputQuery: (request: unknown, enabled: boolean) => {
        mocks.nodeOutputQuery(request, enabled);

        return {data: enabled ? mocks.nodeOutput : undefined};
    },
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    ProjectDeploymentKeys: {projectDeployments: ['projectDeployments']},
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: mocks.deployments}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationGettingStartedQuery: (variables: unknown, options: {enabled?: boolean}) => {
        mocks.statusQuery(variables, options);

        return {
            data: options.enabled ? {automationGettingStarted: mocks.status} : undefined,
            isFetchedAfterMount: mocks.fetchedAfterMount,
        };
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

vi.mock('../useEditorGuideActions', () => ({default: () => mocks.actions}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionContentClipboardButton', () => ({
    default: ({value}: {value: string}) => <button aria-label={`Copy ${value}`} type="button" />,
}));

const publishedGuideProject = {
    id: 5,
    lastProjectVersion: 2,
    lastPublishedDate: new Date('2026-11-11T10:00:00Z'),
    projectWorkflowIds: [7],
};

const liveDeployment = {
    enabled: true,
    id: 9,
    projectDeploymentWorkflows: [{enabled: true, workflowId: 'w1'}],
};

const renderCard = (projectWorkflowId = 7) =>
    render(
        <MemoryRouter>
            <EditorGettingStartedCard projectId={5} projectWorkflowId={projectWorkflowId} />
        </MemoryRouter>
    );

describe('EditorGettingStartedCard', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        useProjectDeploymentWorkflowSheetStore.setState({projectDeploymentWorkflowSheetOpen: false});
        localStorage.setItem(
            'bytechef.automation-getting-started.guide-workflow',
            JSON.stringify({projectId: 5, projectWorkflowId: 7})
        );

        mocks.actions.runPending = false;
        mocks.actions.runWaiting = false;
        mocks.canCreateProject = true;
        mocks.deployments = [];
        mocks.fetchedAfterMount = true;
        mocks.nodeOutput = undefined;
        mocks.projects = [{id: 5, projectWorkflowIds: [7]}];
        mocks.status = {completed: false};
        mocks.tasks = [];
    });

    it('renders nothing when the open workflow is not the guide workflow', () => {
        renderCard(8);

        expect(screen.queryByText('Getting started')).not.toBeInTheDocument();
        expect(mocks.statusQuery.mock.calls.every(([, options]) => options.enabled === false)).toBe(true);
    });

    it('renders nothing and does not request the status without PROJECT_CREATE', () => {
        mocks.canCreateProject = false;

        renderCard();

        expect(screen.queryByText('Getting started')).not.toBeInTheDocument();
        expect(mocks.statusQuery.mock.calls.every(([, options]) => options.enabled === false)).toBe(true);
    });

    it('renders nothing when the guide is already complete at load', () => {
        mocks.status = {completed: true};

        renderCard();

        expect(screen.queryByText('Getting started')).not.toBeInTheDocument();
        expect(screen.queryByText("You're set up")).not.toBeInTheDocument();
    });

    it('shows the title and progress from the editor contents', () => {
        mocks.tasks = [
            {parameters: {uri: 'https://dummyjson.com/quotes/random'}, type: 'httpClient/v1/get'},
            {parameters: {text: '${httpClient_1.quote}'}, type: 'logger/v1/info'},
        ];

        renderCard();

        expect(screen.getByText('Getting started')).toBeInTheDocument();
        expect(screen.getByText('2/8')).toBeInTheDocument();
    });

    it('hides the values to paste once their steps are done', () => {
        mocks.tasks = [
            {parameters: {uri: 'https://dummyjson.com/quotes/random'}, type: 'httpClient/v1/get'},
            {parameters: {text: '${httpClient_1.quote}'}, type: 'logger/v1/info'},
        ];

        renderCard();

        expect(screen.queryByLabelText('Copy https://dummyjson.com/quotes/random')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Copy ${httpClient_1.quote}')).not.toBeInTheDocument();
    });

    it('shows the values to paste next to their copy buttons', () => {
        renderCard();

        expect(screen.getByText('https://dummyjson.com/quotes/random')).toBeInTheDocument();
        expect(screen.getByLabelText('Copy https://dummyjson.com/quotes/random')).toBeInTheDocument();
        expect(screen.getByText('${httpClient_1.quote}')).toBeInTheDocument();
        expect(screen.getByLabelText('Copy ${httpClient_1.quote}')).toBeInTheDocument();
        expect(screen.getByText('Or paste')).toBeInTheDocument();
    });

    it('collapses to a pill and remembers it', async () => {
        const user = userEvent.setup();

        mocks.tasks = [
            {parameters: {uri: 'https://dummyjson.com/quotes/random'}, type: 'httpClient/v1/get'},
            {parameters: {text: '${httpClient_1.quote}'}, type: 'logger/v1/info'},
        ];

        renderCard();

        await user.click(screen.getByLabelText('Collapse getting started'));

        expect(screen.queryByText('Fetch a quote')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Expand getting started')).toHaveTextContent('Getting started 2/8');
        expect(localStorage.getItem('bytechef.automation-getting-started.editor-guide-collapsed')).toBe('true');

        await user.click(screen.getByLabelText('Expand getting started'));

        expect(screen.getByText('Fetch a quote')).toBeInTheDocument();
    });

    it('offers collapse but no close button', () => {
        renderCard();

        expect(screen.getByLabelText('Collapse getting started')).toBeInTheDocument();
        expect(screen.queryByLabelText('Close getting started')).not.toBeInTheDocument();
    });

    it('opens the publish popover from the Publish button', async () => {
        const user = userEvent.setup();

        renderCard();

        await user.click(screen.getByRole('button', {name: 'Publish'}));

        expect(mocks.actions.openPublish).toHaveBeenCalled();
    });

    it('keeps Deploy disabled until the workflow is published', async () => {
        const user = userEvent.setup();

        const {unmount} = renderCard();

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();

        unmount();

        mocks.projects = [publishedGuideProject];
        mocks.status = {completed: false, published: true};

        renderCard();

        await user.click(screen.getByRole('button', {name: 'Deploy'}));

        expect(mocks.actions.openDeploy).toHaveBeenCalled();
    });

    it('keeps Publish open and Deploy disabled when only another project was published', () => {
        mocks.status = {completed: false, published: true};

        renderCard();

        expect(screen.getByRole('button', {name: 'Publish'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
    });

    it('ticks publish from the tenant-wide status and keeps Deploy open without a deployment in this environment', () => {
        mocks.deployments = [];
        mocks.projects = [publishedGuideProject];
        mocks.status = {completed: false, deployed: true, live: true, published: true};

        renderCard();

        expect(screen.getByText('4/8')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Deploy'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'Run now'})).not.toBeInTheDocument();
    });

    it('offers Run now when the deployment is live and nothing has run', async () => {
        const user = userEvent.setup();

        mocks.deployments = [liveDeployment];
        mocks.status = {completed: false, deployed: true, live: true, published: true};

        renderCard();

        expect(screen.getByText('Waiting for the first run')).toBeInTheDocument();
        expect(screen.queryByText('Click Run now.')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Run now'}));

        expect(mocks.actions.runNow).toHaveBeenCalled();

        expect(screen.getByRole('button', {name: 'View executions'})).toBeDisabled();
    });

    it('shows a spinner instead of Run now while waiting for the requested run', () => {
        mocks.actions.runWaiting = true;
        mocks.deployments = [liveDeployment];
        mocks.status = {completed: false, deployed: true, live: true, published: true};

        renderCard();

        expect(screen.getByTestId('run-waiting')).toHaveTextContent('Waiting...');
        expect(screen.queryByRole('button', {name: 'Run now'})).not.toBeInTheDocument();
        expect(screen.queryByText('Waiting for the first run')).not.toBeInTheDocument();
    });

    it('lets the user run again after the requested run failed', () => {
        mocks.actions.runWaiting = true;
        mocks.deployments = [liveDeployment];
        mocks.status = {
            completed: false,
            deployed: true,
            lastFailedRun: {errorMessage: 'boom', status: 'FAILED'},
            live: true,
            published: true,
        };

        renderCard();

        expect(screen.getByTestId('last-failed-run')).toBeInTheDocument();
        expect(screen.queryByTestId('run-waiting')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Run now'})).toBeEnabled();
    });

    it('keeps every step ticked and shows the run result once a run exists', async () => {
        const user = userEvent.setup();

        mocks.deployments = [liveDeployment];

        const view = renderCard();

        mocks.status = {
            completed: false,
            firstRun: {projectDeploymentEnabled: true, projectDeploymentId: '9'},
        };

        view.rerender(
            <MemoryRouter>
                <EditorGettingStartedCard projectId={5} projectWorkflowId={7} />
            </MemoryRouter>
        );

        expect(screen.getByText('Getting started')).toBeInTheDocument();
        expect(screen.getByText('7/8')).toBeInTheDocument();
        expect(screen.getAllByLabelText('Done')).toHaveLength(7);
        expect(screen.getByText('Run received')).toBeInTheDocument();
        expect(screen.getByText('Your workflow ran.')).toBeInTheDocument();
        expect(screen.queryByText('View workflow executions')).not.toBeInTheDocument();
        expect(screen.getByText('See executions')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Run now'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Disable deployment'})).not.toBeInTheDocument();
        expect(screen.getByText('In Deployments, click the workflow, or click View executions')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'View executions'}));

        expect(mocks.actions.openExecutions).toHaveBeenCalled();

        act(() => {
            useProjectDeploymentWorkflowSheetStore.setState({projectDeploymentWorkflowSheetOpen: true});
        });

        expect(screen.getByText("You're set up")).toBeInTheDocument();
        expect(screen.getByText('8/8')).toBeInTheDocument();
    });

    it('does not latch the guide from cached data that was not fetched after mount', () => {
        mocks.fetchedAfterMount = false;
        mocks.status = {completed: true};

        renderCard();

        expect(screen.queryByText('Getting started')).not.toBeInTheDocument();
    });

    it('ticks Test it once the HTTP Client node output has a test response', () => {
        mocks.tasks = [
            {name: 'httpClient_1', parameters: {uri: 'https://dummyjson.com/quotes/random'}, type: 'httpClient/v1/get'},
        ];
        mocks.nodeOutput = {testOutputResponse: {sampleOutput: {quote: 'q'}}};

        renderCard();

        expect(screen.getByText('2/8')).toBeInTheDocument();
        expect(mocks.nodeOutputQuery).toHaveBeenCalledWith(
            {environmentId: 1, id: 'workflow-1', workflowNodeName: 'httpClient_1'},
            true
        );
    });

    it('does not tick Test it without a test response on the HTTP Client node output', () => {
        mocks.tasks = [
            {name: 'httpClient_1', parameters: {uri: 'https://dummyjson.com/quotes/random'}, type: 'httpClient/v1/get'},
        ];
        mocks.nodeOutput = {};

        renderCard();

        expect(screen.getByText('1/8')).toBeInTheDocument();
    });

    it('does not query the node output when there is no HTTP Client task', () => {
        mocks.tasks = [{name: 'logger_1', type: 'logger/v1/info'}];

        renderCard();

        expect(mocks.nodeOutputQuery.mock.calls.every(([, enabled]) => enabled === false)).toBe(true);
    });

    it('does not query the node output when the open workflow is not the guide workflow', () => {
        mocks.tasks = [
            {name: 'httpClient_1', parameters: {uri: 'https://dummyjson.com/quotes/random'}, type: 'httpClient/v1/get'},
        ];

        renderCard(8);

        expect(mocks.nodeOutputQuery.mock.calls.every(([, enabled]) => enabled === false)).toBe(true);
    });

    describe('polling', () => {
        const getRefetchInterval = (status: Record<string, unknown>, error: unknown = null) => {
            const options = mocks.statusQuery.mock.calls.at(-1)?.[1] as {
                refetchInterval: (query: unknown) => number | false;
            };

            return options.refetchInterval({state: {data: {automationGettingStarted: status}, error}});
        };

        beforeEach(() => {
            mocks.deployments = [liveDeployment];
        });

        it('polls every 3000 ms while expanded and no run exists', () => {
            renderCard();

            expect(getRefetchInterval({completed: false})).toBe(3000);
        });

        it('does not poll when collapsed', async () => {
            const user = userEvent.setup();

            renderCard();

            await user.click(screen.getByLabelText('Collapse getting started'));

            expect(getRefetchInterval({completed: false})).toBe(false);
        });

        it('does not poll on error or once a run exists', () => {
            renderCard();

            expect(getRefetchInterval({completed: false}, new Error('failed'))).toBe(false);
            expect(getRefetchInterval({completed: false, firstRun: {projectDeploymentId: '9'}})).toBe(false);
        });

        it('does not poll before anything is live', () => {
            mocks.deployments = [];

            renderCard();

            expect(getRefetchInterval({completed: false})).toBe(false);
        });

        it('polls when a deployment elsewhere in the tenant is live', () => {
            mocks.deployments = [];

            renderCard();

            expect(getRefetchInterval({completed: false, live: true})).toBe(3000);
        });
    });

    it('refreshes the status after the guide project is published', () => {
        const view = renderCard();

        expect(mocks.invalidateQueries).not.toHaveBeenCalled();

        mocks.projects = [{id: 5, lastPublishedDate: new Date('2026-11-11T10:00:00Z'), projectWorkflowIds: [7]}];

        view.rerender(
            <MemoryRouter>
                <EditorGettingStartedCard projectId={5} projectWorkflowId={7} />
            </MemoryRouter>
        );

        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationGettingStarted']});
    });

    it('invalidates the completed query once a first run appears', () => {
        mocks.deployments = [liveDeployment];

        const view = renderCard();

        expect(mocks.invalidateQueries).not.toHaveBeenCalled();

        mocks.status = {completed: false, firstRun: {projectDeploymentEnabled: true, projectDeploymentId: '9'}};

        view.rerender(
            <MemoryRouter>
                <EditorGettingStartedCard projectId={5} projectWorkflowId={7} />
            </MemoryRouter>
        );

        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationGettingStartedCompleted']});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['projectDeployments']});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automation_workflowExecutions']});
    });
});
