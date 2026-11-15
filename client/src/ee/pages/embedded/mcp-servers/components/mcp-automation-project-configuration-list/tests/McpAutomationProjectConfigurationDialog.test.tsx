import {
    McpServer,
    ToolEligibleAutomationProjectConfigurationWorkflowPropertiesDocument,
} from '@/shared/middleware/graphql';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpAutomationProjectConfigurationDialog from '../McpAutomationProjectConfigurationDialog';
import {McpAutomationProjectConfigurationItemType} from '../hooks/useMcpAutomationProjectConfigurationList';

const hoisted = vi.hoisted(() => ({
    configurationsQuery: vi.fn(),
    createMutate: vi.fn(),
    toolPropertiesByWorkflowUuid: {} as Record<string, unknown[]>,
    toolWorkflowUuidsQuery: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useAutomationProjectConfigurationsQuery: hoisted.configurationsQuery,
        useAutomationProjectsQuery: () => ({
            data: {automationProjects: [{configurationCount: 2, id: '10', name: 'Onboarding', published: true}]},
        }),
        useCreateMcpAutomationProjectConfigurationMutation: () => ({
            isPending: false,
            mutate: hoisted.createMutate,
            reset: vi.fn(),
        }),
        useMcpAutomationProjectConfigurationToolWorkflowUuidsQuery: hoisted.toolWorkflowUuidsQuery,
        useToolEligibleAutomationProjectConfigurationWorkflowPropertiesQuery: ({
            workflowUuid,
        }: {
            workflowUuid: string;
        }) => ({
            data: {
                toolEligibleAutomationProjectConfigurationWorkflowProperties:
                    hoisted.toolPropertiesByWorkflowUuid[workflowUuid] ?? [],
            },
            isLoading: false,
        }),
        useUpdateMcpAutomationProjectConfigurationMutation: () => ({
            isPending: false,
            mutate: hoisted.updateMutate,
            reset: vi.fn(),
        }),
    };
});

vi.mock('@/shared/middleware/graphqlFetcher', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphqlFetcher')>();

    return {
        ...actual,
        fetcher: (document: unknown, variables: Record<string, unknown>) => {
            if (document === ToolEligibleAutomationProjectConfigurationWorkflowPropertiesDocument) {
                return () =>
                    Promise.resolve({
                        toolEligibleAutomationProjectConfigurationWorkflowProperties:
                            hoisted.toolPropertiesByWorkflowUuid[variables.workflowUuid as string] ?? [],
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

const toolProperties = [
    {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
    {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
    {controlType: 'TEXT', description: 'The greeting', name: 'greeting', required: true, type: 'STRING'},
];

const mcpServer = {environmentId: '1', id: 's1', name: 'Server'} as McpServer;

const configurations = [
    {
        enabled: true,
        environmentId: '1',
        id: '100',
        name: 'Production config',
        projectId: '10',
        workflows: [
            {enabled: true, label: 'Workflow One', workflowUuid: 'uuid-1'},
            {enabled: false, label: 'Workflow Two', workflowUuid: 'uuid-2'},
        ],
    },
    {
        enabled: true,
        environmentId: '2',
        id: '200',
        name: 'Staging config',
        projectId: '10',
        workflows: [{enabled: true, label: 'Workflow Three', workflowUuid: 'uuid-3'}],
    },
];

const makeEditItem = (
    workflowUuids: string[],
    parameters: Record<string, unknown> | null = null
): McpAutomationProjectConfigurationItemType =>
    ({
        automationProjectConfigurationEnabled: true,
        automationProjectConfigurationId: '100',
        automationProjectConfigurationName: 'Production config',
        id: 'm1',
        mcpServerId: 's1',
        projectId: '10',
        projectVersion: 1,
        workflows: workflowUuids.map((workflowUuid, index) => ({
            available: true,
            enabled: true,
            id: `mw${index}`,
            label: workflowUuid,
            parameters,
            workflowUuid,
        })),
    }) as McpAutomationProjectConfigurationItemType;

describe('McpAutomationProjectConfigurationDialog', () => {
    beforeEach(() => {
        hoisted.configurationsQuery.mockReset();
        hoisted.createMutate.mockReset();
        hoisted.toolWorkflowUuidsQuery.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.toolPropertiesByWorkflowUuid = {'uuid-1': toolProperties};
        hoisted.configurationsQuery.mockReturnValue({
            data: {automationProjectConfigurations: configurations},
        });
        hoisted.toolWorkflowUuidsQuery.mockReturnValue({
            data: {mcpAutomationProjectConfigurationToolWorkflowUuids: ['uuid-1', 'uuid-2']},
        });

        Element.prototype.scrollIntoView = vi.fn();
        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
    });

    it('dialogListsOnlyConfigurationsOfServerEnvironment', async () => {
        render(<McpAutomationProjectConfigurationDialog mcpServer={mcpServer} />);

        const [projectSelect] = screen.getAllByRole('combobox');

        await userEvent.click(projectSelect);
        await userEvent.click(await screen.findByRole('option', {name: 'Onboarding'}));

        const configurationSelect = screen.getAllByRole('combobox')[1];

        await userEvent.click(configurationSelect);

        expect(await screen.findByRole('option', {name: 'Production config'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Staging config'})).not.toBeInTheDocument();

        expect(hoisted.configurationsQuery).toHaveBeenCalledWith(
            expect.objectContaining({environmentId: '1', projectId: '10'}),
            expect.anything()
        );
    });

    it('dialogRendersTurnedOffWorkflowDisabled', async () => {
        render(
            <McpAutomationProjectConfigurationDialog
                mcpAutomationProjectConfiguration={makeEditItem([])}
                mcpServer={mcpServer}
            />
        );

        expect(await screen.findByText('Turned off in the configuration')).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Workflow Two'})).toBeDisabled();
        expect(screen.getByRole('switch', {name: 'Workflow One'})).toBeEnabled();
    });

    it('dialogListsOnlyWorkflowsWithWorkflowCallTrigger', async () => {
        hoisted.toolWorkflowUuidsQuery.mockReturnValue({
            data: {mcpAutomationProjectConfigurationToolWorkflowUuids: ['uuid-1']},
        });

        render(
            <McpAutomationProjectConfigurationDialog
                mcpAutomationProjectConfiguration={makeEditItem([])}
                mcpServer={mcpServer}
            />
        );

        expect(await screen.findByRole('switch', {name: 'Workflow One'})).toBeInTheDocument();
        expect(screen.queryByRole('switch', {name: 'Workflow Two'})).not.toBeInTheDocument();

        expect(hoisted.toolWorkflowUuidsQuery).toHaveBeenCalledWith(
            {automationProjectConfigurationId: '100'},
            expect.objectContaining({enabled: true})
        );
    });

    it('dialogSubmitsSelectedWorkflowUuids', async () => {
        render(
            <McpAutomationProjectConfigurationDialog
                mcpAutomationProjectConfiguration={makeEditItem(['uuid-1'])}
                mcpServer={mcpServer}
            />
        );

        await waitFor(() => expect(screen.getByRole('switch', {name: 'Workflow One'})).toBeChecked());

        await userEvent.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0]).toEqual({
            id: 'm1',
            input: {workflowConfigurations: [], workflowUuids: ['uuid-1']},
        });
    });

    it('dialogShowsSavedToolFieldsAndSendsChangedParameters', async () => {
        render(
            <McpAutomationProjectConfigurationDialog
                mcpAutomationProjectConfiguration={makeEditItem(['uuid-1'], {greeting: 'hello', toolName: 'saved'})}
                mcpServer={mcpServer}
            />
        );

        expect(await screen.findByLabelText('greeting')).toHaveValue('hello');
        expect(screen.getByLabelText('toolName')).toHaveValue('saved');

        await userEvent.type(screen.getByLabelText('toolDescription'), 'Greets');

        await userEvent.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0].input.workflowConfigurations).toEqual([
            {parameters: {greeting: 'hello', toolDescription: 'Greets', toolName: 'saved'}, workflowUuid: 'uuid-1'},
        ]);
    });

    it('dialogCreateSendsWorkflowConfigurations', async () => {
        render(<McpAutomationProjectConfigurationDialog mcpServer={mcpServer} />);

        await userEvent.click(screen.getAllByRole('combobox')[0]);
        await userEvent.click(await screen.findByRole('option', {name: 'Onboarding'}));
        await userEvent.click(screen.getAllByRole('combobox')[1]);
        await userEvent.click(await screen.findByRole('option', {name: 'Production config'}));

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await userEvent.click(await screen.findByRole('switch', {name: 'Workflow One'}));

        await userEvent.type(await screen.findByLabelText('toolName'), 'tool_one');

        await userEvent.click(screen.getByRole('button', {name: 'Add'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        const {input} = hoisted.createMutate.mock.calls[0][0];

        expect(input).toMatchObject({
            automationProjectConfigurationId: '100',
            mcpServerId: 's1',
            workflowUuids: ['uuid-1'],
        });
        expect(input.workflowConfigurations).toHaveLength(1);
        expect(input.workflowConfigurations[0].workflowUuid).toBe('uuid-1');
        expect(input.workflowConfigurations[0].parameters.toolName).toBe('tool_one');
        expect(input.workflowConfigurations[0].parameters.greeting).toMatch(/^=fromAi\('greeting'/);
    });
});
