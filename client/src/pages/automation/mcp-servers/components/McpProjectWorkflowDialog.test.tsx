import {McpServer} from '@/shared/middleware/graphql';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpProjectWorkflowDialog from './McpProjectWorkflowDialog';
import {McpProjectItemType} from './mcp-project-workflow-list/hooks/useMcpProjectList';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants — vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    eligibleResult: {data: undefined} as {data: unknown},
    projectDeployment: undefined as unknown,
    projectVersionWorkflows: [] as unknown[],
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useCreateMcpProjectMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
        useToolEligibleProjectVersionWorkflowsQuery: () => hoisted.eligibleResult,
        useUpdateMcpProjectMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
    };
});

vi.mock('@/shared/queries/automation/projects.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projects.queries')>()),
    useGetWorkspaceProjectsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectWorkflows.queries')>()),
    useGetProjectVersionWorkflowsQuery: (_id: number, _version: number, _all: boolean, enabled: boolean) => ({
        data: enabled ? hoisted.projectVersionWorkflows : undefined,
    }),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectDeployments.queries')>()),
    useGetProjectDeploymentQuery: () => ({data: hoisted.projectDeployment}),
}));

vi.mock('@/shared/queries/automation/connections.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/connections.queries')>()),
    useGetWorkspaceConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/components/ConnectionConfigurationList', () => ({
    default: () => <div data-testid="connection-configuration-list" />,
}));

vi.mock('@/shared/components/InputConfigurationList', () => ({
    default: () => <div data-testid="input-configuration-list" />,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

// Replace the project/version selectors with simple buttons that invoke their
// onChange synchronously, so we can drive the create-mode flow deterministically.
vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox',
    () => ({
        default: ({onChange}: {onChange: (item: {label: string; name: string; value: number}) => void}) => (
            <button onClick={() => onChange({label: 'Project', name: 'Project', value: 10})} type="button">
                pick-project
            </button>
        ),
    })
);

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect',
    () => ({
        default: ({onChange}: {onChange: (value: number) => void}) => (
            <button onClick={() => onChange(2)} type="button">
                pick-version
            </button>
        ),
    })
);

const mcpServer = {environmentId: '1', id: 's1', name: 'Server'} as McpServer;

describe('McpProjectWorkflowDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.eligibleResult.data = undefined;
        hoisted.projectDeployment = undefined;
        hoisted.projectVersionWorkflows = [{id: 'w1', inputs: [], label: 'Workflow One', tasks: [], triggers: []}];

        Element.prototype.scrollIntoView = vi.fn();
    });

    it('keeps Next disabled on the basic step until a project version is picked', async () => {
        const user = userEvent.setup();

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        expect(screen.getByText(/New Workflow Call trigger/i, {selector: 'p'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        await user.click(screen.getByText('pick-project'));
        await user.click(screen.getByText('pick-version'));

        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('shows an explanatory message and keeps Add disabled when the selected project version has no tool-eligible workflows', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {toolEligibleProjectVersionWorkflows: []};

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await user.click(screen.getByText('pick-project'));
        await user.click(screen.getByText('pick-version'));
        await user.click(screen.getByRole('button', {name: 'Next'}));

        expect(screen.getByText(/No tool-eligible workflows found/i)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add'})).toBeDisabled();
    });

    it('opens on the workflows step and keeps Update disabled in edit mode when the project version no longer has tool-eligible workflows', () => {
        hoisted.eligibleResult.data = {toolEligibleProjectVersionWorkflows: []};

        const mcpProject = {
            id: 'mp1',
            mcpProjectWorkflows: [{id: 'mpw1', workflow: {id: 'w1', label: 'Workflow One'}}],
            mcpServerId: 's1',
            project: {id: '10', name: 'Project'},
            projectVersion: 2,
        } as unknown as McpProjectItemType;

        render(<McpProjectWorkflowDialog mcpProject={mcpProject} mcpServer={mcpServer} />);

        expect(screen.getByText(/No tool-eligible workflows found/i)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Update'})).toBeDisabled();
    });

    it('enables Add and submits once a tool-eligible workflow is toggled on', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await user.click(screen.getByText('pick-project'));
        await user.click(screen.getByText('pick-version'));
        await user.click(screen.getByRole('button', {name: 'Next'}));

        const addButton = screen.getByRole('button', {name: 'Add'});

        expect(addButton).toBeDisabled();

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));

        expect(addButton).toBeEnabled();

        await user.click(addButton);

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.createMutate.mock.calls[0][0]).toMatchObject({
            input: {
                mcpServerId: 's1',
                projectId: '10',
                projectVersion: 2,
                selectedWorkflowIds: ['w1'],
                workflowConfigurations: [{connections: [], inputs: {}, workflowId: 'w1'}],
            },
        });
    });

    it('shows the Connections and Inputs of a toggled-on workflow', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await user.click(screen.getByText('pick-project'));
        await user.click(screen.getByText('pick-version'));
        await user.click(screen.getByRole('button', {name: 'Next'}));

        expect(screen.queryByRole('tab', {name: /Connections/})).not.toBeInTheDocument();

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));

        expect(screen.getByRole('tab', {name: /Connections/})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: /Inputs/})).toBeInTheDocument();
    });

    it('submits the saved connections and inputs of the system deployment in edit mode', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };
        hoisted.projectDeployment = {
            environmentId: 1,
            id: 30,
            projectDeploymentWorkflows: [
                {
                    connections: [{connectionId: 5, workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'}],
                    inputs: {channel: 'general'},
                    workflowId: 'w1',
                },
            ],
        };
        hoisted.projectVersionWorkflows = [
            {
                id: 'w1',
                inputs: [{name: 'channel'}],
                label: 'Workflow One',
                tasks: [
                    {
                        connections: [
                            {
                                componentName: 'slack',
                                componentVersion: 1,
                                key: 'slack',
                                required: true,
                                workflowNodeName: 'slack_1',
                            },
                        ],
                        name: 'slack_1',
                        type: 'slack/v1/send',
                    },
                ],
                triggers: [],
            },
        ];

        const mcpProject = {
            id: 'mp1',
            mcpProjectWorkflows: [{id: 'mpw1', workflow: {id: 'w1', label: 'Workflow One'}}],
            mcpServerId: 's1',
            project: {id: '10', name: 'Project'},
            projectDeploymentId: '30',
            projectVersion: 2,
        } as unknown as McpProjectItemType;

        render(<McpProjectWorkflowDialog mcpProject={mcpProject} mcpServer={mcpServer} />);

        await user.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0]).toEqual({
            id: 'mp1',
            input: {
                selectedWorkflowIds: ['w1'],
                workflowConfigurations: [
                    {
                        connections: [{connectionId: '5', workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'}],
                        inputs: {channel: 'general'},
                        workflowId: 'w1',
                    },
                ],
            },
        });
    });
});
