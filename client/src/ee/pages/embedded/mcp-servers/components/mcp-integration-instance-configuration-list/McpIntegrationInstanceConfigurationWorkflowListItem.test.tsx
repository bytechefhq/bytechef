import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import McpIntegrationInstanceConfigurationWorkflowListItem from './McpIntegrationInstanceConfigurationWorkflowListItem';
import {McpIntegrationInstanceConfigurationWorkflowItemType} from './hooks/useMcpIntegrationInstanceConfigurationList';

vi.mock('./hooks/useMcpIntegrationInstanceConfigurationWorkflowListItem', () => ({
    default: () => ({
        handleCloseEditDialog: vi.fn(),
        handleConfirmDelete: vi.fn(),
        integrationInstanceConfigurationWorkflow: undefined,
        isDeletePending: false,
        setShowDeleteDialog: vi.fn(),
        setShowEditWorkflowDialog: vi.fn(),
        showDeleteDialog: false,
        showEditWorkflowDialog: false,
        workflow: undefined,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog',
    () => ({
        default: () => null,
    })
);

vi.mock('./McpIntegrationInstanceConfigurationWorkflowPropertiesPopover', () => ({
    default: () => null,
}));

const renderListItem = (
    mcpIntegrationInstanceConfigurationWorkflow: McpIntegrationInstanceConfigurationWorkflowItemType
) =>
    render(
        <McpActivePopoverProvider>
            <McpIntegrationInstanceConfigurationWorkflowListItem
                componentName="gmail"
                mcpIntegrationInstanceConfigurationWorkflow={mcpIntegrationInstanceConfigurationWorkflow}
            />
        </McpActivePopoverProvider>
    );

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpIntegrationInstanceConfigurationWorkflowListItem', () => {
    it('shows a workflow icon before the workflow label', () => {
        const {container} = renderListItem({
            id: '1',
            workflow: {label: 'Send Email'},
        } as McpIntegrationInstanceConfigurationWorkflowItemType);

        const workflowIcon = container.querySelector('svg.lucide-workflow');
        const workflowLabel = screen.getByText('Send Email');

        expect(workflowIcon).toBeInTheDocument();
        expect(workflowIcon!.compareDocumentPosition(workflowLabel) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('falls back to an unnamed label when the workflow has none', () => {
        renderListItem({id: '1', workflow: {}} as McpIntegrationInstanceConfigurationWorkflowItemType);

        expect(screen.getByText('Unnamed Workflow')).toBeInTheDocument();
    });
});
