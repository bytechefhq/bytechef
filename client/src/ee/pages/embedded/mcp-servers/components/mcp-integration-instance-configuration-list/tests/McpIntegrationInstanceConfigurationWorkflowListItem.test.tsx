import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpIntegrationInstanceConfigurationWorkflowListItem from '../McpIntegrationInstanceConfigurationWorkflowListItem';
import {McpIntegrationInstanceConfigurationWorkflowItemType} from '../hooks/useMcpIntegrationInstanceConfigurationList';

const hoisted = vi.hoisted(() => ({
    openPopover: vi.fn(),
    setShowDeleteDialog: vi.fn(),
    setShowEditWorkflowDialog: vi.fn(),
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', () => ({
    useHasEmbeddedScope: () => true,
}));

vi.mock('../hooks/useMcpIntegrationInstanceConfigurationWorkflowListItem', () => ({
    default: () => ({
        handleCloseEditDialog: vi.fn(),
        handleConfirmDelete: vi.fn(),
        integrationInstanceConfigurationWorkflow: undefined,
        isDeletePending: false,
        setShowDeleteDialog: hoisted.setShowDeleteDialog,
        setShowEditWorkflowDialog: hoisted.setShowEditWorkflowDialog,
        showDeleteDialog: false,
        showEditWorkflowDialog: false,
        workflow: undefined,
    }),
}));

vi.mock('@/shared/contexts/McpActivePopoverContext', () => ({
    useCloseActivePopoverOnUnmount: vi.fn(),
    useMcpActivePopover: () => ({activePopoverId: null, closePopover: vi.fn(), openPopover: hoisted.openPopover}),
}));

const mcpIntegrationInstanceConfigurationWorkflow = {
    id: '9',
    workflow: {label: 'Sync contacts'},
} as unknown as McpIntegrationInstanceConfigurationWorkflowItemType;

const chooseAction = async (actionName: string) => {
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', {name: 'Sync contacts actions'}));
    await user.click(await screen.findByRole('menuitem', {name: actionName}));
};

describe('McpIntegrationInstanceConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.openPopover.mockReset();
        hoisted.setShowDeleteDialog.mockReset();
        hoisted.setShowEditWorkflowDialog.mockReset();

        render(
            <McpIntegrationInstanceConfigurationWorkflowListItem
                componentName="hubspot"
                mcpIntegrationInstanceConfigurationWorkflow={mcpIntegrationInstanceConfigurationWorkflow}
            />
        );
    });

    it('opens the workflow configuration from the Configure button', async () => {
        await userEvent.setup().click(screen.getByRole('button', {name: 'Configure Sync contacts'}));

        expect(hoisted.openPopover).toHaveBeenCalledWith('integration-workflow-9');
    });

    it('opens the edit dialog from the actions menu', async () => {
        await chooseAction('Edit');

        expect(hoisted.setShowEditWorkflowDialog).toHaveBeenCalledWith(true);
    });

    it('asks for confirmation before deleting from the actions menu', async () => {
        await chooseAction('Delete');

        expect(hoisted.setShowDeleteDialog).toHaveBeenCalledWith(true);
    });
});
