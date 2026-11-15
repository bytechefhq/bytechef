import {TooltipProvider} from '@/components/ui/tooltip';
import useAutomationExecutionsSheetStore from '@/ee/pages/embedded/automations/stores/useAutomationExecutionsSheetStore';
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
    workflowId: 'workflow-id-1',
    workflowUuid: 'uuid-1',
} as McpAutomationProjectConfigurationWorkflowItemType;

const renderListItem = (listItemWorkflow = workflow, environmentId: string | undefined = '0') =>
    render(
        <TooltipProvider>
            <McpAutomationProjectConfigurationWorkflowListItem
                environmentId={environmentId}
                projectName="Project One"
                workflow={listItemWorkflow}
            />
        </TooltipProvider>
    );

describe('McpAutomationProjectConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.enabledMutate.mockReset();

        useAutomationExecutionsSheetStore.setState({
            automationExecutionsSheetOpen: false,
            environmentId: undefined,
            projectName: undefined,
            workflow: undefined,
        });
    });

    it('switchCallsEnabledMutation', async () => {
        renderListItem();

        await userEvent.click(screen.getByRole('switch', {name: 'Enable Workflow One'}));

        expect(hoisted.enabledMutate).toHaveBeenCalledWith({enabled: false, id: 'w1'});
    });

    it('configureOpensThePropertiesPopover', async () => {
        renderListItem();

        await userEvent.click(screen.getByRole('button', {name: 'Configure Workflow One'}));

        expect(await screen.findByTestId('properties-popover')).toBeInTheDocument();
    });

    it('workflowNameOpensTheExecutionsSheet', async () => {
        renderListItem();

        await userEvent.click(screen.getByRole('button', {name: 'Workflow One'}));

        const state = useAutomationExecutionsSheetStore.getState();

        expect(state.automationExecutionsSheetOpen).toBe(true);
        expect(state.environmentId).toBe(0);
        expect(state.projectName).toBe('Project One');
        expect(state.workflow).toEqual({label: 'Workflow One', workflowId: 'workflow-id-1', workflowUuid: 'uuid-1'});
    });

    it('workflowNameIsDisabledWithoutTheWorkflowId', () => {
        renderListItem({...workflow, workflowId: null});

        expect(screen.getByRole('button', {name: 'Workflow One'})).toBeDisabled();
    });
});
