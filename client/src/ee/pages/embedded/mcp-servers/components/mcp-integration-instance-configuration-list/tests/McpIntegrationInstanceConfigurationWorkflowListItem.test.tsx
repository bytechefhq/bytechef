import {TooltipProvider} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpIntegrationInstanceConfigurationWorkflowListItem from '../McpIntegrationInstanceConfigurationWorkflowListItem';
import {McpIntegrationInstanceConfigurationWorkflowItemType} from '../hooks/useMcpIntegrationInstanceConfigurationList';

const hoisted = vi.hoisted(() => ({
    handleEnabledChange: vi.fn(),
    integrationInstanceConfigurationWorkflow: undefined as {integrationInstanceConfigurationId: number} | undefined,
    openPopover: vi.fn(),
    setShowDeleteDialog: vi.fn(),
    setShowEditWorkflowDialog: vi.fn(),
    workflow: undefined as {id: string; label: string; workflowTaskComponentNames: string[]} | undefined,
}));

vi.mock('../hooks/useMcpIntegrationInstanceConfigurationWorkflowListItem', () => ({
    default: () => ({
        handleCloseEditDialog: vi.fn(),
        handleConfirmDelete: vi.fn(),
        handleEnabledChange: hoisted.handleEnabledChange,
        integrationInstanceConfigurationWorkflow: hoisted.integrationInstanceConfigurationWorkflow,
        isDeletePending: false,
        isEnablePending: false,
        setShowDeleteDialog: hoisted.setShowDeleteDialog,
        setShowEditWorkflowDialog: hoisted.setShowEditWorkflowDialog,
        showDeleteDialog: false,
        showEditWorkflowDialog: false,
        workflow: hoisted.workflow,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/hooks/useWorkflowComponentDefinitions', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useWorkflowComponentDefinitions')>()),
    default: () => ({workflowComponentDefinitions: {}, workflowTaskDispatcherDefinitions: {}}),
}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: ({filteredComponentNames}: {filteredComponentNames?: string[]}) => (
        <div data-testid="trigger-and-components-row">{filteredComponentNames?.join(',')}</div>
    ),
}));

vi.mock('@/shared/contexts/McpActivePopoverContext', () => ({
    useCloseActivePopoverOnUnmount: vi.fn(),
    useMcpActivePopover: () => ({activePopoverId: null, closePopover: vi.fn(), openPopover: hoisted.openPopover}),
}));

const mcpIntegrationInstanceConfigurationWorkflow = {
    enabled: true,
    id: '9',
    workflow: {label: 'Sync contacts'},
} as unknown as McpIntegrationInstanceConfigurationWorkflowItemType;

const chooseAction = async (actionName: string) => {
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', {name: 'Sync contacts actions'}));
    await user.click(await screen.findByRole('menuitem', {name: actionName}));
};

const renderListItem = () =>
    render(
        <TooltipProvider>
            <McpIntegrationInstanceConfigurationWorkflowListItem
                componentName="hubspot"
                integrationInstanceConfigurationName="HubSpot Sales"
                integrationVersion={3}
                mcpIntegrationInstanceConfigurationWorkflow={mcpIntegrationInstanceConfigurationWorkflow}
            />
        </TooltipProvider>
    );

describe('McpIntegrationInstanceConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.handleEnabledChange.mockReset();
        hoisted.integrationInstanceConfigurationWorkflow = undefined;
        hoisted.openPopover.mockReset();
        hoisted.setShowDeleteDialog.mockReset();
        hoisted.setShowEditWorkflowDialog.mockReset();
        hoisted.workflow = undefined;

        useIntegrationInstanceConfigurationWorkflowSheetStore.setState({
            connectedUserId: undefined,
            integrationInstanceConfigurationId: undefined,
            integrationInstanceConfigurationName: undefined,
            integrationInstanceConfigurationWorkflowSheetOpen: false,
            integrationVersion: undefined,
            workflow: undefined,
        });
    });

    it("opens the workflow's executions when the workflow is clicked", async () => {
        hoisted.integrationInstanceConfigurationWorkflow = {integrationInstanceConfigurationId: 44};
        hoisted.workflow = {id: 'workflow-a', label: 'Sync contacts', workflowTaskComponentNames: ['hubspot']};

        renderListItem();

        expect(screen.getByTestId('trigger-and-components-row')).toHaveTextContent('hubspot');

        await userEvent.setup().click(screen.getByRole('button', {name: 'Sync contacts'}));

        const state = useIntegrationInstanceConfigurationWorkflowSheetStore.getState();

        expect(state.integrationInstanceConfigurationWorkflowSheetOpen).toBe(true);
        expect(state.integrationInstanceConfigurationId).toBe(44);
        expect(state.integrationInstanceConfigurationName).toBe('HubSpot Sales');
        expect(state.integrationVersion).toBe(3);
        expect(state.workflow).toEqual(hoisted.workflow);
    });

    it('switches the workflow tool off from its toggle', async () => {
        renderListItem();

        const enabledSwitch = screen.getByRole('switch', {name: 'Enable Sync contacts'});

        expect(enabledSwitch).toBeChecked();

        await userEvent.setup().click(enabledSwitch);

        expect(hoisted.handleEnabledChange).toHaveBeenCalledWith(false);
    });

    it('keeps the workflow name inert until the workflow is loaded', () => {
        renderListItem();

        expect(screen.getByRole('button', {name: 'Sync contacts'})).toBeDisabled();
        expect(screen.queryByTestId('trigger-and-components-row')).not.toBeInTheDocument();
    });

    it('opens the workflow configuration from the Configure button', async () => {
        renderListItem();

        await userEvent.setup().click(screen.getByRole('button', {name: 'Configure Sync contacts'}));

        expect(hoisted.openPopover).toHaveBeenCalledWith('integration-workflow-9');
    });

    it('opens the edit dialog from the actions menu', async () => {
        renderListItem();

        await chooseAction('Edit');

        expect(hoisted.setShowEditWorkflowDialog).toHaveBeenCalledWith(true);
    });

    it('asks for confirmation before deleting from the actions menu', async () => {
        renderListItem();

        await chooseAction('Delete');

        expect(hoisted.setShowDeleteDialog).toHaveBeenCalledWith(true);
    });
});
