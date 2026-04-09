import {Separator} from '@/components/ui/separator';
import IntegrationBreadcrumb from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb';
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
import {onlineManager, useIsFetching} from '@tanstack/react-query';
import {RefObject} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

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

    const isFetching = useIsFetching();
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

    const isOnline = onlineManager.isOnline();

    if (!integration) {
        return <IntegrationSkeleton />;
    }

    return (
        <header
            className={twMerge(
                'flex items-center justify-between bg-surface-main px-3 py-2.5',
                leftSidebarOpen && 'pl-0 pr-3',
                copilotLayoutShifted && 'pr-0'
            )}
        >
            <div className="flex items-center">
                <LeftSidebarButton onLeftSidebarOpenClick={() => setLeftSidebarOpen(!leftSidebarOpen)} />

                <Separator className="ml-2 mr-4 h-4" orientation="vertical" />

                {integrationWorkflows && (
                    <IntegrationBreadcrumb
                        currentWorkflow={workflow as Workflow}
                        integration={integration}
                        integrationWorkflowId={integrationWorkflowId}
                        integrationWorkflows={integrationWorkflows}
                        onIntegrationWorkflowValueChange={handleIntegrationWorkflowValueChange}
                    />
                )}
            </div>

            <div className="flex items-center">
                <LoadingIndicator isFetching={isFetching} isOnline={isOnline} />

                <SettingsMenu
                    integration={integration}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflow={workflow as Workflow}
                />

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

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
            </div>
        </header>
    );
};

export default IntegrationHeader;
