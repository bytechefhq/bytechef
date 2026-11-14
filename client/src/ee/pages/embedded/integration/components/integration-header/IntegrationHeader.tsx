import {ButtonGroup} from '@/components/ui/button-group';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import IntegrationConfigurationsSheet from '@/ee/pages/embedded/integration/components/IntegrationConfigurationsSheet';
import IntegrationBreadcrumb from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb';
import IntegrationBreadcrumbSkeleton from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumbSkeleton';
import IntegrationItemSelect from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationItemSelect';
import IntegrationSkeleton from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationSkeleton';
import OutputPanelButton from '@/ee/pages/embedded/integration/components/integration-header/components/OutputButton';
import SettingsMenu from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/SettingsMenu';
import {useIntegrationHeader} from '@/ee/pages/embedded/integration/components/integration-header/hooks/useIntegrationHeader';
import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import ConfigurationsListButton from '@/ee/pages/embedded/shared/ConfigurationsListButton';
import ConfigureButton from '@/ee/pages/embedded/shared/ConfigureButton';
import {isIntegrationConfigurable} from '@/ee/pages/embedded/shared/integrationPublication';
import {IntegrationInstanceConfiguration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import CodeWorkflowHeaderActions, {
    CodeWorkflowSaveButton,
} from '@/pages/platform/code-workflow/components/CodeWorkflowHeaderActions';
import WorkflowActionsButton from '@/pages/platform/workflow-editor/components/WorkflowActionsButton';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import PublishPopover from '@/shared/components/PublishPopover';
import useCopilotCompactHeader from '@/shared/components/copilot/hooks/useCopilotCompactHeader';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import LeftSidebarButton from '@/shared/layout/LeftSidebarButton';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsMutating} from '@tanstack/react-query';
import {RefObject, useState, useSyncExternalStore} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const getOnlineStatus = () => onlineManager.isOnline();

const subscribeToOnlineStatus = (onOnlineStatusChange: () => void) => onlineManager.subscribe(onOnlineStatusChange);

interface IntegrationHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    integrationId: number;
    integrationWorkflowId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const IntegrationHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    integrationId,
    integrationWorkflowId,
    runDisabled,
    updateWorkflowMutation,
}: IntegrationHeaderProps) => {
    const [configurationDialogOpen, setConfigurationDialogOpen] = useState(false);
    const [configurationsSheetOpen, setConfigurationsSheetOpen] = useState(false);

    const copilotLayoutShifted = useCopilotLayoutShifted();

    useCopilotCompactHeader();
    const {leftSidebarOpen, setLeftSidebarOpen} = useIntegrationsLeftSidebarStore(
        useShallow((state) => ({
            leftSidebarOpen: state.leftSidebarOpen,
            setLeftSidebarOpen: state.setLeftSidebarOpen,
        }))
    );
    const {workflowIsRunning} = useWorkflowEditorStore(
        useShallow((state) => ({
            workflowIsRunning: state.workflowIsRunning,
        }))
    );
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const isOnline = useSyncExternalStore(subscribeToOnlineStatus, getOnlineStatus);
    const isSaving = useIsMutating();
    const {
        handleIntegrationValueChange,
        handleIntegrationWorkflowValueChange,
        handlePublishIntegrationSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        hasUnpublishedChanges,
        integration,
        integrationWorkflows,
        integrations,
        publishIntegrationMutationIsPending,
    } = useIntegrationHeader({
        bottomResizablePanelRef,
        integrationId,
    });

    const loadingIndicator = (isSaving > 0 || !isOnline) && (
        <LoadingIndicator isFetching={isSaving} isOnline={isOnline} showLabel />
    );

    if (!integration) {
        return <IntegrationSkeleton />;
    }

    return (
        <header
            className={twMerge(
                'flex items-center justify-between bg-surface-main px-3 py-2.5 transition-[padding] duration-300 ease-in-out',
                leftSidebarOpen && 'pr-3 pl-0',
                copilotLayoutShifted && 'pr-0'
            )}
        >
            <div className="flex items-center gap-2">
                <LeftSidebarButton
                    onLeftSidebarOpenClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                    tooltip="See integrations"
                />

                {integrationWorkflows ? (
                    <IntegrationBreadcrumb
                        integration={integration}
                        integrations={integrations}
                        itemSelect={
                            <IntegrationItemSelect
                                currentIntegrationWorkflowId={integrationWorkflowId}
                                currentLabel={workflow?.label}
                                integrationWorkflows={integrationWorkflows}
                                onWorkflowValueChange={handleIntegrationWorkflowValueChange}
                            />
                        }
                        onIntegrationValueChange={handleIntegrationValueChange}
                    />
                ) : (
                    <IntegrationBreadcrumbSkeleton />
                )}

                {loadingIndicator}
            </div>

            <div className="flex items-center gap-1">
                {integration?.codeWorkflow && <CodeWorkflowHeaderActions />}

                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    leadingAction={integration?.codeWorkflow ? <CodeWorkflowSaveButton /> : undefined}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />

                <ButtonGroup>
                    <PublishPopover
                        disabled={!hasUnpublishedChanges}
                        isPending={publishIntegrationMutationIsPending}
                        onPublishSubmit={handlePublishIntegrationSubmit}
                        title="Publish Integration"
                        tooltip="Publish the integration"
                    />

                    <ConfigureButton
                        disabledReason={
                            isIntegrationConfigurable(integration)
                                ? undefined
                                : 'Publish the integration before configuring it.'
                        }
                        onClick={() => setConfigurationDialogOpen(true)}
                    />

                    <ConfigurationsListButton
                        label="Integration Configurations"
                        onClick={() => setConfigurationsSheetOpen(true)}
                    />
                </ButtonGroup>

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <SettingsMenu
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    integration={integration}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflow={workflow as Workflow}
                />
            </div>

            {configurationDialogOpen && (
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={
                        {
                            environmentId: DEVELOPMENT_ENVIRONMENT,
                            integrationId: integration.id,
                        } as IntegrationInstanceConfiguration
                    }
                    onClose={() => setConfigurationDialogOpen(false)}
                    onSuccess={() => setConfigurationsSheetOpen(true)}
                    showTabs
                />
            )}

            {configurationsSheetOpen && (
                <IntegrationConfigurationsSheet
                    integration={integration}
                    onSheetOpenChange={(open) => !open && setConfigurationsSheetOpen(false)}
                    sheetOpen
                />
            )}
        </header>
    );
};

export default IntegrationHeader;
