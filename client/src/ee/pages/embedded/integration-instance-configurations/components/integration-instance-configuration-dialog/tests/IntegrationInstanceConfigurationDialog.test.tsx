import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialog from '../IntegrationInstanceConfigurationDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useCreateIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
    useUpdateIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    IntegrationInstanceConfigurationKeys: {integrationInstanceConfigurations: ['integrationInstanceConfigurations']},
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries', () => ({
    IntegrationInstanceConfigurationTagKeys: {integrationInstanceConfigurationTags: ['tags']},
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: [], isPending: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {integrations: ['integrations']},
    useGetIntegrationQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore', () => ({
    useWorkflowsEnabledStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({reset: vi.fn(), setWorkflowEnabled: vi.fn(), workflowEnabledMap: new Map()}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 1}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationInstanceConfigurationCreated: vi.fn()}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../IntegrationInstanceConfigurationDialogBasicStep', () => ({
    default: () => <div data-testid="basic-step" />,
}));

vi.mock('../IntegrationInstanceConfigurationDialogWorkflowsStep', () => ({
    default: () => <div data-testid="workflows-step" />,
}));

vi.mock('@/shared/components/connection/ConnectionParameters', () => ({
    default: () => <div data-testid="connection-parameters" />,
}));

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IntegrationInstanceConfigurationDialog', () => {
    describe('create mode', () => {
        it('should title the dialog for a new configuration and name the current step', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('heading', {name: /New Instance Configuration/})).toBeInTheDocument();
            expect(screen.getByRole('heading', {name: /Basic/})).toBeInTheDocument();
        });

        it('should render the basic step', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByTestId('basic-step')).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('should title the dialog for an existing configuration', () => {
            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={{
                        id: 1,
                        integrationId: 2,
                        integrationVersion: 1,
                        name: 'Existing',
                    }}
                    onClose={onClose}
                />
            );

            expect(screen.getByRole('heading', {name: /Edit Instance Configuration/})).toBeInTheDocument();
        });
    });

    describe('version upgrade', () => {
        it('should title the dialog for a version upgrade', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} updateIntegrationVersion />);

            expect(screen.getByRole('heading', {name: 'Upgrade Integration Version'})).toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close and cancel controls', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of any form submission', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            // closeDialog defers onClose through a setTimeout so the form can reset first.
            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
        });
    });
});
