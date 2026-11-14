import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpAutomationProjectConfigurationWorkflowListItem from '../McpAutomationProjectConfigurationWorkflowListItem';
import {McpAutomationProjectConfigurationWorkflowItemType} from '../hooks/useMcpAutomationProjectConfigurationList';

const hoisted = vi.hoisted(() => ({
    enabledMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useUpdateMcpAutomationProjectConfigurationWorkflowEnabledMutation: () => ({
            isPending: false,
            mutate: hoisted.enabledMutate,
        }),
    };
});

vi.mock('../McpAutomationProjectConfigurationWorkflowPropertiesPopover', () => ({
    default: () => <div data-testid="properties-popover" />,
}));

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
    });

    it('switchCallsEnabledMutation', async () => {
        render(<McpAutomationProjectConfigurationWorkflowListItem workflow={workflow} />);

        await userEvent.click(screen.getByRole('switch', {name: 'Enable Workflow One'}));

        expect(hoisted.enabledMutate).toHaveBeenCalledWith({enabled: false, id: 'w1'});
    });

    it('configureOpensThePropertiesPopover', async () => {
        render(<McpAutomationProjectConfigurationWorkflowListItem workflow={workflow} />);

        await userEvent.click(screen.getByRole('button', {name: 'Configure Workflow One'}));

        expect(await screen.findByTestId('properties-popover')).toBeInTheDocument();
    });
});
