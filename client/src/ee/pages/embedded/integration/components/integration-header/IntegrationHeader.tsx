import IntegrationBreadcrumb from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb';
import IntegrationItemSelect from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationItemSelect';
import IntegrationSkeleton from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationSkeleton';
import LeftSidebarButton from '@/ee/pages/embedded/integration/components/integration-header/components/LeftSidebarButton';
import OutputPanelButton from '@/ee/pages/embedded/integration/components/integration-header/components/OutputButton';
import PublishPopover from '@/ee/pages/embedded/integration/components/integration-header/components/PublishPopover';
import WorkflowActionsButton from '@/ee/pages/embedded/integration/components/integration-header/components/WorkflowActionsButton';
import SettingsMenu from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/SettingsMenu';
import {useIntegrationHeader} from '@/ee/pages/embedded/integration/components/integration-header/hooks/useIntegrationHeader';
import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsMutating} from '@tanstack/react-query';
import {RefObject, useSyncExternalStore} from 'react';
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
    const copilotLayoutShifted = useCopilotLayoutShifted();
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
        handleIntegrationWorkflowValueChange,
        handlePublishIntegrationSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        integration,
        integrationWorkflows,
        publishIntegrationMutationIsPending,
    } = useIntegrationHeader({
        bottomResizablePanelRef,
        integrationId,
    });

    const loadingIndicator = (isSaving > 0 || !isOnline) && (
        <LoadingIndicator className="size-6 rounded-full" isFetching={isSaving} isOnline={isOnline} />
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
                <LeftSidebarButton onLeftSidebarOpenClick={() => setLeftSidebarOpen(!leftSidebarOpen)} />

                {integrationWorkflows && (
                    <IntegrationBreadcrumb
                        integration={integration}
                        itemSelect={
                            <IntegrationItemSelect
                                currentIntegrationWorkflowId={integrationWorkflowId}
                                currentLabel={workflow?.label}
                                integrationWorkflows={integrationWorkflows}
                                onWorkflowValueChange={handleIntegrationWorkflowValueChange}
                            />
                        }
                    />
                )}

                {loadingIndicator}
            </div>

            <div className="flex items-center gap-1">
                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />

                <PublishPopover
                    isPending={publishIntegrationMutationIsPending}
                    onPublishIntegrationSubmit={handlePublishIntegrationSubmit}
                />

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <SettingsMenu
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    integration={integration}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflow={workflow as Workflow}
                />
            </div>
        </header>
    );
};

export default IntegrationHeader;
