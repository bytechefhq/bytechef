import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpProjectWorkflowListItem from './McpProjectWorkflowListItem';
import {McpProjectWorkflowItemType} from './hooks/useMcpProjectList';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteMcpProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateMcpProjectWorkflowMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string; input: {enabled: boolean}}) => {
            hoisted.updateMutate(variables);

            options?.onSuccess?.();
        },
    }),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/contexts/McpActivePopoverContext', () => ({
    useCloseActivePopoverOnUnmount: vi.fn(),
    useMcpActivePopover: () => ({activePopoverId: null, closePopover: vi.fn(), openPopover: vi.fn()}),
}));

const createMcpProjectWorkflow = (enabled: boolean) =>
    ({
        enabled,
        id: '9',
        mcpProjectId: 3,
        projectDeploymentWorkflowId: 4,
        workflow: {id: 'workflow-a', label: 'Lead intake'},
    }) as unknown as McpProjectWorkflowItemType;

describe('McpProjectWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockReset();
        hoisted.updateMutate.mockReset();
    });

    it('disables the workflow tool from its toggle', async () => {
        const user = userEvent.setup();

        render(<McpProjectWorkflowListItem mcpProjectWorkflow={createMcpProjectWorkflow(true)} />);

        const enabledSwitch = screen.getByRole('switch', {name: 'Enable Lead intake'});

        expect(enabledSwitch).toBeChecked();

        await user.click(enabledSwitch);

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledWith({id: '9', input: {enabled: false}}));

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['mcpProjectsByServerId']});
    });

    it('shows a disabled workflow tool as switched off', () => {
        render(<McpProjectWorkflowListItem mcpProjectWorkflow={createMcpProjectWorkflow(false)} />);

        expect(screen.getByRole('switch', {name: 'Enable Lead intake'})).not.toBeChecked();
    });
});
