import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpAutomationProjectConfigurationWorkflowListItem from '../McpAutomationProjectConfigurationWorkflowListItem';
import {McpAutomationProjectConfigurationWorkflowItemType} from '../hooks/useMcpAutomationProjectConfigurationList';

const hoisted = vi.hoisted(() => ({
    enabledMutate: vi.fn(),
    parametersMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useUpdateMcpAutomationProjectConfigurationWorkflowEnabledMutation: () => ({
            isPending: false,
            mutate: hoisted.enabledMutate,
        }),
        useUpdateMcpAutomationProjectConfigurationWorkflowParametersMutation: () => ({
            isPending: false,
            mutate: hoisted.parametersMutate,
        }),
    };
});

const workflow = {
    available: true,
    enabled: true,
    id: 'w1',
    label: 'Workflow One',
    parameters: {customerId: 'fromAi(customerId)', toolName: 'old_name'},
    workflowUuid: 'uuid-1',
} as McpAutomationProjectConfigurationWorkflowItemType;

describe('McpAutomationProjectConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.enabledMutate.mockReset();
        hoisted.parametersMutate.mockReset();
    });

    it('switchCallsEnabledMutation', async () => {
        render(<McpAutomationProjectConfigurationWorkflowListItem workflow={workflow} />);

        await userEvent.click(screen.getByRole('switch', {name: 'Enable Workflow One'}));

        expect(hoisted.enabledMutate).toHaveBeenCalledWith({enabled: false, id: 'w1'});
    });

    it('toolParametersEditSubmitsParametersMutation', async () => {
        render(<McpAutomationProjectConfigurationWorkflowListItem workflow={workflow} />);

        await userEvent.click(screen.getByRole('button', {name: 'Configure Workflow One'}));

        const toolNameInput = await screen.findByLabelText('Tool Name');

        await userEvent.clear(toolNameInput);
        await userEvent.type(toolNameInput, 'new_name');
        await userEvent.type(screen.getByLabelText('Tool Description'), 'Does a thing');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.parametersMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.parametersMutate.mock.calls[0][0]).toEqual({
            id: 'w1',
            parameters: {
                customerId: 'fromAi(customerId)',
                toolDescription: 'Does a thing',
                toolName: 'new_name',
            },
        });
    });
});
