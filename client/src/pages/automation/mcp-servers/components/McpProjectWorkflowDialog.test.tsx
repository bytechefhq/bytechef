import {McpServer, ToolEligibleWorkflowPropertiesDocument} from '@/shared/middleware/graphql';
import {render, screen, waitFor, within} from '@/shared/util/test-utils';
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
    pendingToolPropertiesLoad: null as Promise<unknown> | null,
    projectDeployment: undefined as unknown,
    projectVersionWorkflows: [] as unknown[],
    toolPropertiesByWorkflowId: {} as Record<string, unknown[]>,
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useCreateMcpProjectMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
        useToolEligibleProjectVersionWorkflowsQuery: () => hoisted.eligibleResult,
        useToolEligibleWorkflowPropertiesQuery: ({workflowId}: {workflowId: string}) => ({
            data: {toolEligibleWorkflowProperties: hoisted.toolPropertiesByWorkflowId[workflowId] ?? []},
            isLoading: false,
        }),
        useUpdateMcpProjectMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
    };
});

vi.mock('@/shared/middleware/graphqlFetcher', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphqlFetcher')>();

    return {
        ...actual,
        fetcher: (document: unknown, variables: Record<string, unknown>) => {
            if (document === ToolEligibleWorkflowPropertiesDocument) {
                if (hoisted.pendingToolPropertiesLoad) {
                    return () => hoisted.pendingToolPropertiesLoad;
                }

                return () =>
                    Promise.resolve({
                        toolEligibleWorkflowProperties:
                            hoisted.toolPropertiesByWorkflowId[variables.workflowId as string] ?? [],
                    });
            }

            return actual.fetcher(document as never, variables as never);
        },
    };
});

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', async () => {
    const {Controller} = await import('react-hook-form');

    return {
        default: ({control, properties}: {control: never; properties: Array<{name: string}>}) => (
            <div>
                {properties.map((property) => (
                    <Controller
                        control={control}
                        key={property.name}
                        name={property.name}
                        render={({field}) => (
                            <input
                                aria-label={property.name}
                                onChange={field.onChange}
                                value={(field.value as string | undefined) ?? ''}
                            />
                        )}
                    />
                ))}
            </div>
        ),
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

const toolProperties = [
    {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
    {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
    {controlType: 'TEXT', description: 'The channel', name: 'channel', required: true, type: 'STRING'},
    {controlType: 'TEXT', defaultValue: 'hello', name: 'greeting', type: 'STRING'},
];

const openWorkflowsStep = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.click(screen.getByText('pick-project'));
    await user.click(screen.getByText('pick-version'));
    await user.click(screen.getByRole('button', {name: 'Next'}));
};

describe('McpProjectWorkflowDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.eligibleResult.data = undefined;
        hoisted.pendingToolPropertiesLoad = null;
        hoisted.projectDeployment = undefined;
        hoisted.projectVersionWorkflows = [{id: 'w1', inputs: [], label: 'Workflow One', tasks: [], triggers: []}];
        hoisted.toolPropertiesByWorkflowId = {w1: toolProperties};

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
                toolParameters: [],
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

    it('shows a tool tab with tool name description and schema fields', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await openWorkflowsStep(user);

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));

        await user.click(screen.getByRole('tab', {name: /Tool/}));

        const toolPanel = screen.getByRole('tabpanel');

        expect(await within(toolPanel).findByLabelText('toolName')).toBeInTheDocument();
        expect(within(toolPanel).getByLabelText('toolDescription')).toBeInTheDocument();
        expect(within(toolPanel).getByLabelText('channel')).toBeInTheDocument();
        expect(within(toolPanel).getByRole('button', {name: 'Let AI fill all'})).toBeInTheDocument();

        await waitFor(() =>
            expect((within(toolPanel).getByLabelText('channel') as HTMLInputElement).value).toMatch(
                /^=fromAi\('channel'/
            )
        );
    });

    it('does not show ai controls on the inputs tab', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await openWorkflowsStep(user);

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));

        await user.click(screen.getByRole('tab', {name: /Inputs/}));

        expect(screen.getByTestId('input-configuration-list')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Let AI fill all'})).not.toBeInTheDocument();
        expect(screen.queryByText(/fromAi/)).not.toBeInTheDocument();
    });

    it('sends seeded tool parameters for new workflows', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await openWorkflowsStep(user);

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));

        await user.click(screen.getByRole('button', {name: 'Add'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        const {toolParameters} = hoisted.createMutate.mock.calls[0][0].input;

        expect(toolParameters).toHaveLength(1);
        expect(toolParameters[0].workflowId).toBe('w1');
        expect(toolParameters[0].parameters.channel).toMatch(/^=fromAi\('channel'/);
        expect(toolParameters[0].parameters).not.toHaveProperty('greeting');
        expect(toolParameters[0].parameters).not.toHaveProperty('toolName');
    });

    it('prefills the tool tab from saved parameters in edit mode', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };

        const mcpProject = {
            id: 'mp1',
            mcpProjectWorkflows: [
                {
                    id: 'mpw1',
                    parameters: {channel: 'general', toolName: 'Saved tool'},
                    workflow: {id: 'w1', label: 'Workflow One'},
                },
            ],
            mcpServerId: 's1',
            project: {id: '10', name: 'Project'},
            projectVersion: 2,
        } as unknown as McpProjectItemType;

        render(<McpProjectWorkflowDialog mcpProject={mcpProject} mcpServer={mcpServer} />);

        await user.click(screen.getByRole('tab', {name: /Tool/}));

        const toolPanel = screen.getByRole('tabpanel');

        expect(await within(toolPanel).findByLabelText('channel')).toHaveValue('general');
        expect(within(toolPanel).getByLabelText('toolName')).toHaveValue('Saved tool');
        expect(within(toolPanel).getByLabelText('greeting')).toHaveValue('hello');

        await user.type(within(toolPanel).getByLabelText('toolDescription'), 'Posts');

        await user.click(screen.getByRole('tab', {name: /Inputs/}));

        expect(screen.queryByLabelText('toolDescription')).not.toBeInTheDocument();

        await user.click(screen.getByRole('tab', {name: /Tool/}));

        expect(await screen.findByLabelText('toolDescription')).toHaveValue('Posts');

        await user.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0].input.toolParameters).toEqual([
            {
                parameters: {channel: 'general', toolDescription: 'Posts', toolName: 'Saved tool'},
                workflowId: 'w1',
            },
        ]);
    });

    it('drops tool parameters of workflows toggled off', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [
                {id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}},
                {id: 'pw2', workflow: {id: 'w2', label: 'Workflow Two'}},
            ],
        };
        hoisted.projectVersionWorkflows = [
            {id: 'w1', inputs: [], label: 'Workflow One', tasks: [], triggers: []},
            {id: 'w2', inputs: [], label: 'Workflow Two', tasks: [], triggers: []},
        ];
        hoisted.toolPropertiesByWorkflowId = {w1: toolProperties, w2: toolProperties};

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await openWorkflowsStep(user);

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));
        await user.click(screen.getByRole('tab', {name: /Tool/}));
        await user.type(await screen.findByLabelText('toolDescription'), 'Posts');

        expect(screen.getByLabelText('toolDescription')).toHaveValue('Posts');

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));
        await user.click(screen.getByRole('switch', {name: 'Workflow Two'}));
        await user.click(screen.getByRole('switch', {name: 'Workflow Two'}));
        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));
        await user.click(screen.getByRole('tab', {name: /Tool/}));

        await waitFor(() =>
            expect((screen.getByLabelText('channel') as HTMLInputElement).value).toMatch(/^=fromAi\('channel'/)
        );

        expect(screen.getByLabelText('toolDescription')).toHaveValue('');

        await user.click(screen.getByRole('button', {name: 'Add'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        const {selectedWorkflowIds, toolParameters} = hoisted.createMutate.mock.calls[0][0].input;

        expect(selectedWorkflowIds).toEqual(['w1']);
        expect(toolParameters.map((toolParameter: {workflowId: string}) => toolParameter.workflowId)).toEqual(['w1']);
        expect(toolParameters[0].parameters).not.toHaveProperty('toolDescription');
    });

    it('waits for pending tool property loads before saving', async () => {
        const user = userEvent.setup();

        let resolveToolProperties: (value: unknown) => void = () => undefined;

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };
        hoisted.pendingToolPropertiesLoad = new Promise((resolve) => {
            resolveToolProperties = resolve;
        });

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await openWorkflowsStep(user);

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));
        await user.click(screen.getByRole('button', {name: 'Add'}));

        expect(await screen.findByRole('button', {name: /Saving/})).toBeInTheDocument();

        expect(hoisted.createMutate).not.toHaveBeenCalled();

        resolveToolProperties({toolEligibleWorkflowProperties: toolProperties});

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        const {toolParameters} = hoisted.createMutate.mock.calls[0][0].input;

        expect(toolParameters).toHaveLength(1);
        expect(toolParameters[0].parameters.channel).toMatch(/^=fromAi\('channel'/);
    });

    it('seeds a workflow added in edit mode but not the existing one', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [
                {id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}},
                {id: 'pw2', workflow: {id: 'w2', label: 'Workflow Two'}},
            ],
        };
        hoisted.projectVersionWorkflows = [
            {id: 'w1', inputs: [], label: 'Workflow One', tasks: [], triggers: []},
            {id: 'w2', inputs: [], label: 'Workflow Two', tasks: [], triggers: []},
        ];
        hoisted.toolPropertiesByWorkflowId = {w1: toolProperties, w2: toolProperties};

        const mcpProject = {
            id: 'mp1',
            mcpProjectWorkflows: [{id: 'mpw1', parameters: {}, workflow: {id: 'w1', label: 'Workflow One'}}],
            mcpServerId: 's1',
            project: {id: '10', name: 'Project'},
            projectVersion: 2,
        } as unknown as McpProjectItemType;

        render(<McpProjectWorkflowDialog mcpProject={mcpProject} mcpServer={mcpServer} />);

        await user.click(screen.getByRole('tab', {name: /Tool/}));
        await user.type(await screen.findByLabelText('toolDescription'), 'Posts');

        await user.click(screen.getByRole('switch', {name: 'Workflow Two'}));

        await user.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        const {selectedWorkflowIds, toolParameters} = hoisted.updateMutate.mock.calls[0][0].input;

        expect(selectedWorkflowIds).toEqual(['w1', 'w2']);
        expect(toolParameters).toHaveLength(2);
        expect(toolParameters[0]).toEqual({parameters: {toolDescription: 'Posts'}, workflowId: 'w1'});
        expect(toolParameters[1].workflowId).toBe('w2');
        expect(toolParameters[1].parameters.channel).toMatch(/^=fromAi\('channel'/);
    });
});
