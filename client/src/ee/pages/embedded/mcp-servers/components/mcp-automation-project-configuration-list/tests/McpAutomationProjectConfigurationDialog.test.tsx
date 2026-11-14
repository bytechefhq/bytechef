import {McpServer} from '@/shared/middleware/graphql';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpAutomationProjectConfigurationDialog from '../McpAutomationProjectConfigurationDialog';
import {McpAutomationProjectConfigurationItemType} from '../hooks/useMcpAutomationProjectConfigurationList';

const hoisted = vi.hoisted(() => ({
    configurationsQuery: vi.fn(),
    createMutate: vi.fn(),
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
        useUpdateMcpAutomationProjectConfigurationMutation: () => ({
            isPending: false,
            mutate: hoisted.updateMutate,
            reset: vi.fn(),
        }),
    };
});

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

const makeEditItem = (workflowUuids: string[]): McpAutomationProjectConfigurationItemType =>
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
            parameters: null,
            workflowUuid,
        })),
    }) as McpAutomationProjectConfigurationItemType;

describe('McpAutomationProjectConfigurationDialog', () => {
    beforeEach(() => {
        hoisted.configurationsQuery.mockReset();
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.configurationsQuery.mockReturnValue({
            data: {automationProjectConfigurations: configurations},
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
            input: {workflowConfigurations: [{workflowUuid: 'uuid-1'}], workflowUuids: ['uuid-1']},
        });
    });

    it('dialogCreateSendsWorkflowConfigurations', async () => {
        render(<McpAutomationProjectConfigurationDialog mcpServer={mcpServer} />);

        await userEvent.click(screen.getAllByRole('combobox')[0]);
        await userEvent.click(await screen.findByRole('option', {name: 'Onboarding'}));
        await userEvent.click(screen.getAllByRole('combobox')[1]);
        await userEvent.click(await screen.findByRole('option', {name: 'Production config'}));

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await userEvent.click(await screen.findByRole('switch', {name: 'Workflow One'}));

        await userEvent.type(await screen.findByLabelText('Workflow One tool name'), 'tool_one');

        await userEvent.click(screen.getByRole('button', {name: 'Add'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.createMutate.mock.calls[0][0]).toEqual({
            input: {
                automationProjectConfigurationId: '100',
                mcpServerId: 's1',
                workflowConfigurations: [{parameters: {toolName: 'tool_one'}, workflowUuid: 'uuid-1'}],
                workflowUuids: ['uuid-1'],
            },
        });
    });
});
