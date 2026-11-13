import {
    McpServer,
    ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesDocument,
} from '@/shared/middleware/graphql';
import {render, screen, waitFor, within} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpIntegrationInstanceConfigurationWorkflowDialog from './McpIntegrationInstanceConfigurationWorkflowDialog';
import {McpIntegrationInstanceConfigurationItemType} from './mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants — vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    eligibleResult: {data: undefined} as {data: unknown},
    pendingToolPropertiesLoad: null as Promise<unknown> | null,
    toolPropertiesByWorkflowId: {} as Record<string, unknown[]>,
    updateMutate: vi.fn(),
    versionWorkflows: [] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useCreateMcpIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
        useToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery: ({
            workflowId,
        }: {
            workflowId: string;
        }) => ({
            data: {
                toolEligibleIntegrationInstanceConfigurationWorkflowProperties:
                    hoisted.toolPropertiesByWorkflowId[workflowId] ?? [],
            },
            isLoading: false,
        }),
        useToolEligibleIntegrationInstanceConfigurationWorkflowsQuery: () => hoisted.eligibleResult,
        useUpdateMcpIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
    };
});

vi.mock('@/shared/middleware/graphqlFetcher', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphqlFetcher')>();

    return {
        ...actual,
        fetcher: (document: unknown, variables: Record<string, unknown>) => {
            if (document === ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesDocument) {
                if (hoisted.pendingToolPropertiesLoad) {
                    return () => hoisted.pendingToolPropertiesLoad;
                }

                return () =>
                    Promise.resolve({
                        toolEligibleIntegrationInstanceConfigurationWorkflowProperties:
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

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: () => ({
        data: {
            id: 42,
            integration: {componentName: 'gmail'},
            integrationId: 7,
            integrationInstanceConfigurationWorkflows: [
                {
                    connections: [{connectionId: 9, workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'}],
                    inputs: {subject: 'Hello'},
                    workflowId: 'w1',
                },
            ],
            integrationVersion: 1,
        },
    }),
    useGetIntegrationInstanceConfigurationsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: hoisted.versionWorkflows}),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections',
    () => ({
        default: () => <div data-testid="connections" />,
    })
);

vi.mock('@/shared/components/InputConfigurationList', () => ({
    default: () => <div data-testid="input-configuration-list" />,
}));

const mcpServer = {id: 's1', name: 'Server'} as McpServer;

const toolProperties = [
    {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
    {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
    {controlType: 'TEXT', description: 'The channel', name: 'channel', required: true, type: 'STRING'},
    {controlType: 'TEXT', defaultValue: 'hello', name: 'greeting', type: 'STRING'},
];

const makeVersionWorkflow = (id: string, label: string) => ({
    id,
    inputs: [],
    label,
    tasks: [
        {
            connections: [
                {componentName: 'gmail', key: 'gmail', required: true, workflowNodeName: 'gmail_1'},
                {componentName: 'slack', key: 'slack', required: true, workflowNodeName: 'slack_1'},
            ],
            name: 'slack_1',
            type: 'slack/v1/send',
        },
    ],
    triggers: [],
});

const makeEditConfig = (
    workflows: Array<{id: string; label: string; parameters?: Record<string, unknown>}>
): McpIntegrationInstanceConfigurationItemType =>
    ({
        id: 'cfg1',
        integration: {name: 'Integration'},
        integrationInstanceConfigurationId: '42',
        integrationVersion: 1,
        mcpIntegrationInstanceConfigurationWorkflows: workflows.map(({id, label, parameters}, index) => ({
            id: `mw${index}`,
            integrationInstanceConfigurationWorkflow: {workflowId: id},
            parameters,
            workflow: {id, label},
        })),
        mcpServerId: 's1',
    }) as unknown as McpIntegrationInstanceConfigurationItemType;

const eligibleWorkflowsOf = (...workflows: Array<{id: string; label: string}>) => ({
    toolEligibleIntegrationInstanceConfigurationWorkflows: workflows,
});

const openToolTab = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.click(screen.getByRole('tab', {name: /Tool/}));
};

describe('McpIntegrationInstanceConfigurationWorkflowDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.eligibleResult.data = undefined;
        hoisted.pendingToolPropertiesLoad = null;
        hoisted.toolPropertiesByWorkflowId = {w1: toolProperties};
        hoisted.versionWorkflows = [makeVersionWorkflow('w1', 'Workflow One')];

        Element.prototype.scrollIntoView = vi.fn();
    });

    it('shows an explanatory message and keeps the submit button disabled when there are no tool-eligible workflows', () => {
        hoisted.eligibleResult.data = {toolEligibleIntegrationInstanceConfigurationWorkflows: []};

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([])}
                mcpServer={mcpServer}
            />
        );

        expect(screen.getByText(/No tool-eligible workflows found/i)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Update'})).toBeDisabled();
    });

    it('enables the submit button once a tool-eligible workflow is selected', async () => {
        hoisted.eligibleResult.data = {
            toolEligibleIntegrationInstanceConfigurationWorkflows: [{id: 'w1', label: 'Workflow One'}],
        };

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([{id: 'w1', label: 'Workflow One'}])}
                mcpServer={mcpServer}
            />
        );

        // Edit mode pre-populates the previously-selected workflow once eligible workflows load.
        await waitFor(() => expect(screen.getByRole('button', {name: 'Update'})).toBeEnabled());

        expect(screen.queryByText(/No tool-eligible workflows found/i)).not.toBeInTheDocument();

        expect(screen.getByRole('switch', {name: 'Workflow One'})).toBeChecked();

        await userEvent.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));
        expect(hoisted.updateMutate.mock.calls[0][0]).toEqual({
            id: 'cfg1',
            input: {
                selectedWorkflowIds: ['w1'],
                toolParameters: [],
                workflowConfigurations: [
                    {
                        connections: [{connectionId: '9', workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'}],
                        inputs: {subject: 'Hello'},
                        workflowId: 'w1',
                    },
                ],
            },
        });
    });

    it('shows a tool tab with tool name description and schema fields', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = eligibleWorkflowsOf({id: 'w1', label: 'Workflow One'});

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([])}
                mcpServer={mcpServer}
            />
        );

        await user.click(await screen.findByRole('switch', {name: 'Workflow One'}));

        await openToolTab(user);

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

        hoisted.eligibleResult.data = eligibleWorkflowsOf({id: 'w1', label: 'Workflow One'});

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([])}
                mcpServer={mcpServer}
            />
        );

        await user.click(await screen.findByRole('switch', {name: 'Workflow One'}));

        await user.click(screen.getByRole('tab', {name: /Inputs/}));

        expect(screen.getByTestId('input-configuration-list')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Let AI fill all'})).not.toBeInTheDocument();
        expect(screen.queryByText(/fromAi/)).not.toBeInTheDocument();
    });

    it('sends seeded tool parameters for new workflows', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = eligibleWorkflowsOf({id: 'w1', label: 'Workflow One'});

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([])}
                mcpServer={mcpServer}
            />
        );

        await user.click(await screen.findByRole('switch', {name: 'Workflow One'}));
        await user.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        const {toolParameters} = hoisted.updateMutate.mock.calls[0][0].input;

        expect(toolParameters).toHaveLength(1);
        expect(toolParameters[0].workflowId).toBe('w1');
        expect(toolParameters[0].parameters.channel).toMatch(/^=fromAi\('channel'/);
        expect(toolParameters[0].parameters).not.toHaveProperty('greeting');
        expect(toolParameters[0].parameters).not.toHaveProperty('toolName');
    });

    it('prefills the tool tab from saved parameters in edit mode', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = eligibleWorkflowsOf({id: 'w1', label: 'Workflow One'});

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([
                    {id: 'w1', label: 'Workflow One', parameters: {channel: 'general', toolName: 'Saved tool'}},
                ])}
                mcpServer={mcpServer}
            />
        );

        await waitFor(() => expect(screen.getByRole('switch', {name: 'Workflow One'})).toBeChecked());

        await openToolTab(user);

        const toolPanel = screen.getByRole('tabpanel');

        expect(await within(toolPanel).findByLabelText('channel')).toHaveValue('general');
        expect(within(toolPanel).getByLabelText('toolName')).toHaveValue('Saved tool');
        expect(within(toolPanel).getByLabelText('greeting')).toHaveValue('hello');

        await user.type(within(toolPanel).getByLabelText('toolDescription'), 'Posts');

        await user.click(screen.getByRole('tab', {name: /Inputs/}));

        expect(screen.queryByLabelText('toolDescription')).not.toBeInTheDocument();

        await openToolTab(user);

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

        hoisted.eligibleResult.data = eligibleWorkflowsOf(
            {id: 'w1', label: 'Workflow One'},
            {id: 'w2', label: 'Workflow Two'}
        );
        hoisted.versionWorkflows = [
            makeVersionWorkflow('w1', 'Workflow One'),
            makeVersionWorkflow('w2', 'Workflow Two'),
        ];
        hoisted.toolPropertiesByWorkflowId = {w1: toolProperties, w2: toolProperties};

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([])}
                mcpServer={mcpServer}
            />
        );

        await user.click(await screen.findByRole('switch', {name: 'Workflow One'}));
        await openToolTab(user);
        await user.type(await screen.findByLabelText('toolDescription'), 'Posts');

        expect(screen.getByLabelText('toolDescription')).toHaveValue('Posts');

        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));
        await user.click(screen.getByRole('switch', {name: 'Workflow Two'}));
        await user.click(screen.getByRole('switch', {name: 'Workflow Two'}));
        await user.click(screen.getByRole('switch', {name: 'Workflow One'}));
        await openToolTab(user);

        await waitFor(() =>
            expect((screen.getByLabelText('channel') as HTMLInputElement).value).toMatch(/^=fromAi\('channel'/)
        );

        expect(screen.getByLabelText('toolDescription')).toHaveValue('');

        await user.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        const {selectedWorkflowIds, toolParameters} = hoisted.updateMutate.mock.calls[0][0].input;

        expect(selectedWorkflowIds).toEqual(['w1']);
        expect(toolParameters.map((toolParameter: {workflowId: string}) => toolParameter.workflowId)).toEqual(['w1']);
        expect(toolParameters[0].parameters).not.toHaveProperty('toolDescription');
    });

    it('waits for pending tool property loads before saving', async () => {
        const user = userEvent.setup();

        let resolveToolProperties: (value: unknown) => void = () => undefined;

        hoisted.eligibleResult.data = eligibleWorkflowsOf({id: 'w1', label: 'Workflow One'});
        hoisted.pendingToolPropertiesLoad = new Promise((resolve) => {
            resolveToolProperties = resolve;
        });

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([])}
                mcpServer={mcpServer}
            />
        );

        await user.click(await screen.findByRole('switch', {name: 'Workflow One'}));
        await user.click(screen.getByRole('button', {name: 'Update'}));

        expect(await screen.findByRole('button', {name: /Saving/})).toBeInTheDocument();

        expect(hoisted.updateMutate).not.toHaveBeenCalled();

        resolveToolProperties({toolEligibleIntegrationInstanceConfigurationWorkflowProperties: toolProperties});

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        const {toolParameters} = hoisted.updateMutate.mock.calls[0][0].input;

        expect(toolParameters).toHaveLength(1);
        expect(toolParameters[0].parameters.channel).toMatch(/^=fromAi\('channel'/);
    });

    it('seeds a workflow added in edit mode but not the existing one', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = eligibleWorkflowsOf(
            {id: 'w1', label: 'Workflow One'},
            {id: 'w2', label: 'Workflow Two'}
        );
        hoisted.versionWorkflows = [
            makeVersionWorkflow('w1', 'Workflow One'),
            makeVersionWorkflow('w2', 'Workflow Two'),
        ];
        hoisted.toolPropertiesByWorkflowId = {w1: toolProperties, w2: toolProperties};

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([
                    {id: 'w1', label: 'Workflow One', parameters: {}},
                ])}
                mcpServer={mcpServer}
            />
        );

        await waitFor(() => expect(screen.getByRole('switch', {name: 'Workflow One'})).toBeChecked());

        await openToolTab(user);
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

    it('selects existing workflows by workflow id in edit mode', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = eligibleWorkflowsOf(
            {id: 'w1', label: 'Shared Label'},
            {id: 'w2', label: 'Shared Label'}
        );
        hoisted.versionWorkflows = [
            makeVersionWorkflow('w1', 'Shared Label'),
            makeVersionWorkflow('w2', 'Shared Label'),
        ];
        hoisted.toolPropertiesByWorkflowId = {w1: toolProperties, w2: toolProperties};

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([
                    {id: 'w1', label: 'Shared Label', parameters: {}},
                ])}
                mcpServer={mcpServer}
            />
        );

        const switches = await screen.findAllByRole('switch', {name: 'Shared Label'});

        await waitFor(() => expect(switches[0]).toBeChecked());

        expect(switches[1]).not.toBeChecked();

        await user.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        const {selectedWorkflowIds, toolParameters} = hoisted.updateMutate.mock.calls[0][0].input;

        expect(selectedWorkflowIds).toEqual(['w1']);
        expect(toolParameters).toEqual([]);
    });
});
