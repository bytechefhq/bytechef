import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationHeader from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeader';
import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, render, screen, userEvent, within} from '@/shared/util/test-utils';
import {onlineManager} from '@tanstack/react-query';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

interface MockIntegrationI {
    id: number;
    integrationWorkflowIds?: number[];
    lastIntegrationVersion?: number;
    lastPublishedDate?: Date;
    name: string;
}

const hoisted = vi.hoisted(() => ({
    configurationDialogProps: vi.fn(),
    configurationsSheetProps: vi.fn(),
    hasUnpublishedChanges: {current: true},
    integration: {current: undefined as MockIntegrationI | undefined},
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
        hasUnpublishedChanges: hoisted.hasUnpublishedChanges.current,
        integration: hoisted.integration.current,
        integrationWorkflows: [{integrationWorkflowId: 11, label: 'Workflow 1'}],
        publishIntegrationMutationIsPending: false,
    }),
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb', () => ({
    default: ({itemSelect}: {itemSelect: React.ReactNode}) => <nav>{itemSelect}</nav>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowActionsButton', () => ({
    default: () => <button>Test</button>,
}));

vi.mock('@/shared/components/PublishPopover', () => ({
    default: ({disabled}: {disabled?: boolean}) => <button disabled={disabled}>Publish</button>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/OutputButton', () => ({
    default: () => <button>Output</button>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/SettingsMenu', () => ({
    default: () => <button>Settings</button>,
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({
        default: (props: Record<string, unknown>) => {
            hoisted.configurationDialogProps(props);

            return <div role="dialog">Configuration Dialog</div>;
        },
    })
);

vi.mock('@/ee/pages/embedded/integration/components/IntegrationConfigurationsSheet', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.configurationsSheetProps(props);

        return <div role="complementary">Configurations Sheet</div>;
    },
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

const unpublishedIntegration: MockIntegrationI = {id: 5, integrationWorkflowIds: [11], name: 'Gmail'};

const publishedIntegration: MockIntegrationI = {
    ...unpublishedIntegration,
    lastIntegrationVersion: 2,
    lastPublishedDate: new Date(),
};

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.hasUnpublishedChanges.current = true;
    hoisted.integration.current = unpublishedIntegration;
    hoisted.isMutating.mockReturnValue(0);

    useIntegrationsLeftSidebarStore.setState({leftSidebarOpen: false});

    onlineManager.setOnline(true);
});

afterEach(() => {
    onlineManager.setOnline(true);
});

it('orders the header actions Test, Publish, Configure, Integration Configurations, Output, Settings', () => {
    renderIntegrationHeader();

    const actionLabels = screen
        .getAllByRole('button')
        .map((button) => button.getAttribute('aria-label') || button.textContent)
        .filter((label) =>
            ['Configure', 'Integration Configurations', 'Output', 'Publish', 'Settings', 'Test'].includes(label ?? '')
        );

    expect(actionLabels).toEqual(['Test', 'Publish', 'Configure', 'Integration Configurations', 'Output', 'Settings']);
});

it('groups Publish, Configure and the configurations list', () => {
    renderIntegrationHeader();

    const buttonGroup = screen.getByRole('group');

    expect(within(buttonGroup).getByRole('button', {name: 'Publish'})).toBeInTheDocument();
    expect(within(buttonGroup).getByRole('button', {name: 'Configure'})).toBeInTheDocument();
    expect(within(buttonGroup).getByRole('button', {name: 'Integration Configurations'})).toBeInTheDocument();
});

it('enables Publish while the integration has unpublished changes', () => {
    renderIntegrationHeader();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeEnabled();
});

it('disables Publish when the integration has no unpublished changes', () => {
    hoisted.hasUnpublishedChanges.current = false;

    renderIntegrationHeader();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
});

it('disables Configure and explains why while the integration with workflows is unpublished', async () => {
    renderIntegrationHeader();

    const configureButton = screen.getByRole('button', {name: 'Configure'});

    expect(configureButton).toBeDisabled();

    await userEvent.hover(configureButton.parentElement!);

    expect(await screen.findByRole('tooltip')).toHaveTextContent('Publish the integration before configuring it.');
});

it('enables Configure for an integration without workflows', () => {
    hoisted.integration.current = {...unpublishedIntegration, integrationWorkflowIds: []};

    renderIntegrationHeader();

    expect(screen.getByRole('button', {name: 'Configure'})).toBeEnabled();
});

it('opens the configuration dialog with tabs for this integration from Configure', async () => {
    hoisted.integration.current = publishedIntegration;

    renderIntegrationHeader();

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Configure'}));

    expect(screen.getByRole('dialog')).toHaveTextContent('Configuration Dialog');
    expect(hoisted.configurationDialogProps).toHaveBeenCalledWith(
        expect.objectContaining({
            integrationInstanceConfiguration: {environmentId: 0, integrationId: 5},
            showTabs: true,
        })
    );
});

it('opens the integration configurations sheet once Configure creates a configuration', async () => {
    hoisted.integration.current = publishedIntegration;

    renderIntegrationHeader();

    await userEvent.click(screen.getByRole('button', {name: 'Configure'}));

    const {onSuccess} = hoisted.configurationDialogProps.mock.lastCall![0] as {onSuccess?: (id: number) => void};

    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();

    act(() => onSuccess?.(42));

    expect(screen.getByRole('complementary')).toHaveTextContent('Configurations Sheet');
    expect(hoisted.configurationsSheetProps).toHaveBeenCalledWith(
        expect.objectContaining({integration: publishedIntegration, sheetOpen: true})
    );
});

it('opens the integration configurations sheet from the list button', async () => {
    renderIntegrationHeader();

    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Integration Configurations'}));

    expect(screen.getByRole('complementary')).toHaveTextContent('Configurations Sheet');
    expect(hoisted.configurationsSheetProps).toHaveBeenCalledWith(
        expect.objectContaining({integration: unpublishedIntegration, sheetOpen: true})
    );
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
