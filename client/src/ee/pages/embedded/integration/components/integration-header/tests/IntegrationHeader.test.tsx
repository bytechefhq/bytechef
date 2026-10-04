import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationHeader from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeader';
import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {onlineManager} from '@tanstack/react-query';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    integration: {current: {id: 5, name: 'Gmail'} as {id: number; name: string} | undefined},
    isMutating: vi.fn(() => 0),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useIsMutating: () => hoisted.isMutating(),
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/hooks/useIntegrationHeader', () => ({
    useIntegrationHeader: () => ({
        handleIntegrationWorkflowValueChange: vi.fn(),
        handlePublishIntegrationSubmit: vi.fn(),
        handleRunClick: vi.fn(),
        handleShowOutputClick: vi.fn(),
        handleStopClick: vi.fn(),
        integration: hoisted.integration.current,
        integrationWorkflows: [{integrationWorkflowId: 11, label: 'Workflow 1'}],
        publishIntegrationMutationIsPending: false,
    }),
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb', () => ({
    default: ({itemSelect}: {itemSelect: React.ReactNode}) => <nav>{itemSelect}</nav>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/WorkflowActionsButton', () => ({
    default: () => <button>Test</button>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/PublishPopover', () => ({
    default: () => <button>Publish</button>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/OutputButton', () => ({
    default: () => <button>Output</button>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/SettingsMenu', () => ({
    default: () => <button>Settings</button>,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({
    default: () => false,
}));

const renderIntegrationHeader = () =>
    render(
        <TooltipProvider>
            <IntegrationHeader
                bottomResizablePanelRef={{current: null}}
                integrationId={5}
                integrationWorkflowId={11}
                runDisabled={false}
                updateWorkflowMutation={{} as UpdateWorkflowMutationType}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    hoisted.integration.current = {id: 5, name: 'Gmail'};
    hoisted.isMutating.mockReturnValue(0);

    useIntegrationsLeftSidebarStore.setState({leftSidebarOpen: false});

    onlineManager.setOnline(true);
});

afterEach(() => {
    onlineManager.setOnline(true);
});

it('orders the header actions Test, Publish, Output, Settings', () => {
    renderIntegrationHeader();

    const actionLabels = screen
        .getAllByRole('button')
        .map((button) => button.textContent)
        .filter((label) => ['Output', 'Publish', 'Settings', 'Test'].includes(label ?? ''));

    expect(actionLabels).toEqual(['Test', 'Publish', 'Output', 'Settings']);
});

it('shows the current workflow in the breadcrumb switcher', () => {
    useWorkflowDataStore.setState({workflow: {...useWorkflowDataStore.getState().workflow, label: 'Workflow 1'}});

    renderIntegrationHeader();

    expect(screen.getByLabelText('Integration item select')).toHaveTextContent('Workflow 1');
});

it('hides the save indicator while nothing is being saved and the app is online', () => {
    renderIntegrationHeader();

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
});

it('shows the save indicator while a mutation is saving', () => {
    hoisted.isMutating.mockReturnValue(1);

    renderIntegrationHeader();

    expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();
});

it('shows the save indicator as soon as the app goes offline', () => {
    renderIntegrationHeader();

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();

    act(() => {
        onlineManager.setOnline(false);
    });

    expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();

    act(() => {
        onlineManager.setOnline(true);
    });

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
});

it('shows a skeleton until the integration loads', () => {
    hoisted.integration.current = undefined;

    renderIntegrationHeader();

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('toggles the integrations sidebar from the sidebar button', async () => {
    renderIntegrationHeader();

    await userEvent.click(screen.getAllByRole('button')[0]);

    expect(useIntegrationsLeftSidebarStore.getState().leftSidebarOpen).toBe(true);

    await userEvent.click(screen.getAllByRole('button')[0]);

    expect(useIntegrationsLeftSidebarStore.getState().leftSidebarOpen).toBe(false);
});
