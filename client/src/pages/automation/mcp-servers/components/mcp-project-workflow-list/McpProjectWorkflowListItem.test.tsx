import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import McpProjectWorkflowListItem from './McpProjectWorkflowListItem';
import {McpProjectWorkflowItemType} from './hooks/useMcpProjectList';

vi.mock('./hooks/useMcpProjectWorkflowBadge', () => ({
    default: () => ({
        handleCloseEditDialog: vi.fn(),
        handleConfirmDelete: vi.fn(),
        isDeletePending: false,
        projectDeploymentWorkflow: undefined,
        setShowDeleteDialog: vi.fn(),
        setShowEditWorkflowDialog: vi.fn(),
        showDeleteDialog: false,
        showEditWorkflowDialog: false,
        workflow: undefined,
    }),
}));

vi.mock('@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog', () => ({
    default: () => null,
}));

vi.mock('./McpProjectWorkflowPropertiesPopover', () => ({
    default: () => null,
}));

const renderListItem = (mcpProjectWorkflow: McpProjectWorkflowItemType) =>
    render(
        <McpActivePopoverProvider>
            <McpProjectWorkflowListItem mcpProjectWorkflow={mcpProjectWorkflow} />
        </McpActivePopoverProvider>
    );

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpProjectWorkflowListItem', () => {
    it('shows a workflow icon before the workflow label', () => {
        const {container} = renderListItem({
            id: '1',
            workflow: {label: 'Send Email'},
        } as McpProjectWorkflowItemType);

        const workflowIcon = container.querySelector('svg.lucide-workflow');
        const workflowLabel = screen.getByText('Send Email');

        expect(workflowIcon).toBeInTheDocument();
        expect(workflowIcon!.compareDocumentPosition(workflowLabel) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('falls back to an unnamed label when the workflow has none', () => {
        renderListItem({id: '1', workflow: {}} as McpProjectWorkflowItemType);

        expect(screen.getByText('Unnamed Workflow')).toBeInTheDocument();
    });
});
