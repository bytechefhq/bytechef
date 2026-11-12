import {TooltipProvider} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ComponentConnection, Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {mockScrollIntoView, render, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowConnectionsSheetContent from './WorkflowConnectionsSheetContent';

const hoisted = vi.hoisted(() => ({
    connectionsByComponentName: {} as Record<string, Array<Record<string, unknown>>>,
    pending: false,
    saveWorkflowConnectionsMock: vi.fn(),
}));

vi.mock('./hooks/useSaveWorkflowConnections', () => ({
    default: () => ({pending: hoisted.pending, saveWorkflowConnections: hoisted.saveWorkflowConnectionsMock}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
        useCreateConnectionMutation: () => ({mutate: vi.fn()}),
        useGetComponentDefinitionsQuery: () => ({data: []}),
        useGetConnectionTagsQuery: () => ({data: []}),
        useGetConnectionsQuery: (request: {componentName: string}) => ({
            data: hoisted.connectionsByComponentName[request.componentName] ?? [],
        }),
    }),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: (request: {componentName: string}) => ({
        data: {componentName: request.componentName, version: 1},
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: () => null,
}));

vi.mock('@/components/ui/sheet', () => ({
    SheetClose: ({children}: {children: ReactNode}) => <>{children}</>,
}));

interface DeferredI {
    promise: Promise<void>;
    resolve: () => void;
}

function createDeferred(): DeferredI {
    let resolve!: () => void;

    const promise = new Promise<void>((resolvePromise) => {
        resolve = resolvePromise;
    });

    return {promise, resolve};
}

function createTask(name: string, componentName: string, componentVersion = 1, required = true) {
    const connection: ComponentConnection = {
        componentName,
        componentVersion,
        key: componentName,
        required,
        workflowNodeName: name,
    };

    return {connections: [connection], label: 'Run', name, type: `${componentName}/v${componentVersion}/run`};
}

function createWorkflow(tasks: object[]): Workflow {
    return {id: 'workflow-1', tasks, triggers: []} as unknown as Workflow;
}

function createTestConfiguration(assignments: Record<string, number>): WorkflowTestConfiguration {
    return {
        connections: Object.entries(assignments).map(([workflowNodeName, connectionId]) => ({
            connectionId,
            workflowConnectionKey: workflowNodeName.split('_')[0],
            workflowNodeName,
        })),
        environmentId: 1,
    };
}

const SUMMARY_WORKFLOW = createWorkflow([
    createTask('googleSheets_1', 'googleSheets'),
    createTask('googleSheets_2', 'googleSheets'),
    createTask('googleSheets_3', 'googleSheets'),
    createTask('googleSheets_4', 'googleSheets'),
    createTask('googleSheets_5', 'googleSheets'),
    createTask('slack_1', 'slack'),
    createTask('slack_2', 'slack'),
    createTask('slack_3', 'slack'),
    createTask('slack_4', 'slack'),
    createTask('slack_5', 'slack'),
]);

const SUMMARY_TEST_CONFIGURATION = createTestConfiguration({
    googleSheets_1: 7,
    googleSheets_2: 7,
    googleSheets_3: 7,
    googleSheets_4: 7,
    googleSheets_5: 9,
    slack_1: 4,
    slack_2: 4,
    slack_3: 4,
    slack_4: 4,
});

function renderContent(
    workflow: Workflow,
    workflowTestConfiguration?: WorkflowTestConfiguration,
    workflowTestConfigurationLoading = false
) {
    const queryClient = new QueryClient();

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{children}</TooltipProvider>
        </QueryClientProvider>
    );

    return render(
        <WorkflowConnectionsSheetContent
            workflow={workflow}
            workflowTestConfiguration={workflowTestConfiguration}
            workflowTestConfigurationLoading={workflowTestConfigurationLoading}
        />,
        {wrapper}
    );
}

beforeEach(() => {
    mockScrollIntoView();
    windowResizeObserver();

    Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
    Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();

    hoisted.pending = false;
    hoisted.connectionsByComponentName = {
        googleSheets: [
            {componentName: 'googleSheets', connectionVersion: 1, id: 7, name: 'Finance Sheets', parameters: {}},
            {componentName: 'googleSheets', connectionVersion: 1, id: 9, name: 'Ops Sheets', parameters: {}},
            {componentName: 'googleSheets', connectionVersion: 1, id: 12, name: 'Marketing Sheets', parameters: {}},
        ],
        slack: [{componentName: 'slack', connectionVersion: 1, id: 4, name: 'Team Slack', parameters: {}}],
    };
    hoisted.saveWorkflowConnectionsMock.mockResolvedValue({savedCount: 0});

    useWorkflowDataStore.setState({
        componentDefinitions: [
            {name: 'googleSheets', title: 'Google Sheets', version: 1},
            {name: 'slack', title: 'Slack', version: 1},
        ],
    });
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('WorkflowConnectionsSheetContent', () => {
    it('renders the summary', () => {
        renderContent(SUMMARY_WORKFLOW, SUMMARY_TEST_CONFIGURATION);

        expect(screen.getByText('9 of 10 connected')).toBeInTheDocument();
        expect(screen.getByText('1 missing')).toBeInTheDocument();
        expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '90');
    });

    it('leaves out the missing count when nothing is missing', () => {
        renderContent(
            createWorkflow([createTask('slack_1', 'slack'), createTask('slack_2', 'slack', 1, false)]),
            createTestConfiguration({slack_1: 4})
        );

        expect(screen.getByText('1 of 2 connected')).toBeInTheDocument();
        expect(screen.queryByText(/missing$/)).not.toBeInTheDocument();
    });

    it('waits for the test configuration before showing connections', () => {
        const {unmount} = renderContent(SUMMARY_WORKFLOW, undefined, true);

        expect(screen.queryByText('0 of 10 connected')).not.toBeInTheDocument();
        expect(screen.queryByRole('group')).not.toBeInTheDocument();
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();

        unmount();

        renderContent(SUMMARY_WORKFLOW, undefined, false);

        expect(screen.getByText('0 of 10 connected')).toBeInTheDocument();
        expect(screen.getAllByRole('group')).toHaveLength(2);
    });

    it('renders one card per connector', () => {
        renderContent(SUMMARY_WORKFLOW, SUMMARY_TEST_CONFIGURATION);

        expect(screen.getAllByRole('group')).toHaveLength(2);
        expect(screen.getByRole('group', {name: 'Google Sheets'})).toBeInTheDocument();
        expect(screen.getByRole('group', {name: 'Slack'})).toBeInTheDocument();
        expect(screen.getByText('5 tasks, 1 override')).toBeInTheDocument();
        expect(screen.getByText('4/5')).toBeInTheDocument();
    });

    it('filters groups', async () => {
        renderContent(SUMMARY_WORKFLOW, SUMMARY_TEST_CONFIGURATION);

        const filterInput = screen.getByRole('searchbox', {name: 'Filter by connector or task'});

        await userEvent.type(filterInput, 'slack');

        expect(screen.getByRole('group', {name: 'Slack'})).toBeInTheDocument();
        expect(screen.queryByRole('group', {name: 'Google Sheets'})).not.toBeInTheDocument();

        await userEvent.clear(filterInput);
        await userEvent.type(filterInput, 'zzz');

        expect(screen.getByText('No connector or task matches "zzz".')).toBeInTheDocument();
        expect(screen.queryByRole('group')).not.toBeInTheDocument();
        expect(screen.getByText('9 of 10 connected')).toBeInTheDocument();
    });

    it('shows the empty state', () => {
        renderContent(createWorkflow([{label: 'Map', name: 'map_1', type: 'map/v1'}]));

        expect(screen.getByText('No connections needed')).toBeInTheDocument();
        expect(
            screen.getByText('No task in this workflow uses a connector that needs a connection.')
        ).toBeInTheDocument();
        expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
        expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    });

    it('sends group changes through the save hook and disables other groups while saving', async () => {
        const save = createDeferred();

        hoisted.saveWorkflowConnectionsMock.mockImplementation(() => {
            hoisted.pending = true;

            return save.promise.then(() => ({savedCount: 4}));
        });

        renderContent(SUMMARY_WORKFLOW, SUMMARY_TEST_CONFIGURATION);

        await userEvent.click(screen.getByRole('combobox', {name: 'Google Sheets connection'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Marketing Sheets'}));

        expect(hoisted.saveWorkflowConnectionsMock).toHaveBeenCalledTimes(1);
        expect(hoisted.saveWorkflowConnectionsMock).toHaveBeenCalledWith([
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_1',
            },
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_3',
            },
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_4',
            },
        ]);

        await waitFor(() =>
            expect(screen.getByRole('group', {name: 'Google Sheets'})).toHaveAttribute('aria-busy', 'true')
        );

        expect(screen.getByRole('group', {name: 'Slack'})).toHaveAttribute('aria-busy', 'false');
        expect(screen.getByRole('combobox', {name: 'Slack connection'})).toBeDisabled();
        expect(screen.getByRole('combobox', {name: 'Google Sheets connection'})).toBeDisabled();

        hoisted.pending = false;

        save.resolve();

        await waitFor(() =>
            expect(screen.getByRole('group', {name: 'Google Sheets'})).toHaveAttribute('aria-busy', 'false')
        );

        expect(screen.getByRole('combobox', {name: 'Slack connection'})).toBeEnabled();
    });

    it('disables the close button while a save is running', () => {
        hoisted.pending = true;

        const {unmount} = renderContent(SUMMARY_WORKFLOW, SUMMARY_TEST_CONFIGURATION);

        expect(screen.getByRole('button', {name: 'Close'})).toBeDisabled();

        unmount();

        hoisted.pending = false;

        renderContent(SUMMARY_WORKFLOW, SUMMARY_TEST_CONFIGURATION);

        expect(screen.getByRole('button', {name: 'Close'})).toBeEnabled();
    });

    it('titles groups that share a component name with their version', () => {
        renderContent(
            createWorkflow([
                createTask('googleSheets_1', 'googleSheets'),
                createTask('googleSheets_2', 'googleSheets', 2),
                createTask('slack_1', 'slack'),
                createTask('acme_1', 'acme'),
            ])
        );

        expect(screen.getByRole('group', {name: 'Google Sheets v1'})).toBeInTheDocument();
        expect(screen.getByRole('group', {name: 'Google Sheets v2'})).toBeInTheDocument();
        expect(screen.getByRole('group', {name: 'Slack'})).toBeInTheDocument();
        expect(screen.getByRole('group', {name: 'acme'})).toBeInTheDocument();
    });
});
