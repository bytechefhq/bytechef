import {Workflow} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationEditWorkflowDialog from '../IntegrationInstanceConfigurationEditWorkflowDialog';

const hoisted = vi.hoisted(() => ({
    updateMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useUpdateIntegrationInstanceConfigurationWorkflowMutation: () => ({
        mutate: hoisted.updateMutate,
        reset: vi.fn(),
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    IntegrationInstanceConfigurationKeys: {integrationInstanceConfigurations: ['integrationInstanceConfigurations']},
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem',
    () => ({
        default: () => <div data-testid="workflows-step-item" />,
    })
);

const onClose = vi.fn();

const defaultProps = {
    componentName: 'github',
    integrationInstanceConfigurationWorkflow: {id: 1, workflowId: 'w1'},
    onClose,
    workflow: {id: 'w1', label: 'Sync Contacts'} as Workflow,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IntegrationInstanceConfigurationEditWorkflowDialog', () => {
    describe('rendering', () => {
        it('should title the dialog after the workflow', () => {
            render(<IntegrationInstanceConfigurationEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByText('Edit Sync Contacts Workflow')).toBeInTheDocument();
        });

        it('should render the workflow configuration step', () => {
            render(<IntegrationInstanceConfigurationEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByTestId('workflows-step-item')).toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<IntegrationInstanceConfigurationEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of any form submission', () => {
            render(<IntegrationInstanceConfigurationEditWorkflowDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationEditWorkflowDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationEditWorkflowDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should update the workflow when save is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationEditWorkflowDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.updateMutate).toHaveBeenCalledTimes(1);
        });
    });
});
