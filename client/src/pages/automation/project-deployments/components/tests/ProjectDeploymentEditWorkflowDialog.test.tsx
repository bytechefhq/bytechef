import {Workflow} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentEditWorkflowDialog from '../ProjectDeploymentEditWorkflowDialog';

const hoisted = vi.hoisted(() => ({
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentWorkflows.mutations', () => ({
    useUpdateProjectDeploymentWorkflowMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    ProjectDeploymentKeys: {projectDeployments: ['projectDeployments']},
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 1}),
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem',
    () => ({
        default: () => <div data-testid="workflows-step-item" />,
    })
);

const onClose = vi.fn();

const defaultProps = {
    onClose,
    projectDeploymentWorkflow: {id: 1, workflowId: 'w1'},
    workflow: {id: 'w1', label: 'Sync Contacts'} as Workflow,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ProjectDeploymentEditWorkflowDialog', () => {
    describe('rendering', () => {
        it('should title the dialog after the workflow', () => {
            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByText('Edit Sync Contacts Workflow')).toBeInTheDocument();
        });

        it('should describe what the dialog configures', () => {
            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByText('Set workflow input, trigger output values and connections.')).toBeInTheDocument();
        });

        it('should render the workflow configuration step', () => {
            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByTestId('workflows-step-item')).toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of any form submission', () => {
            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should update the workflow when save is clicked', async () => {
            const user = userEvent.setup();

            render(<ProjectDeploymentEditWorkflowDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.updateMutate).toHaveBeenCalledTimes(1);
        });
    });
});
