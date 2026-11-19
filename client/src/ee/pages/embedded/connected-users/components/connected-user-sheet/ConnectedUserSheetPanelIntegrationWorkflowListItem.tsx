import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserIntegrationWorkflowEditDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationWorkflowEditDialog';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import McpToolBadge from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/McpToolBadge';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {
    IntegrationInstance,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {IntegrationInstanceKeys} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import isCollapsibleRowClick from '@/shared/components/mcp-server/utils/isCollapsibleRowClick';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {toast} from 'sonner';

const ConnectedUserSheetPanelIntegrationWorkflowListItem = ({
    integrationInstance,
    integrationInstanceConfiguration,
    integrationInstanceConfigurationWorkflow,
    integrationInstanceWorkflow,
    mcpTool = false,
    workflow,
}: {
    integrationInstance: IntegrationInstance;
    integrationInstanceConfiguration: IntegrationInstanceConfiguration;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow;
    integrationInstanceWorkflow?: IntegrationInstanceWorkflow;
    mcpTool?: boolean;
    workflow: Workflow;
}) => {
    const {workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useWorkflowComponentDefinitions(
        useGetComponentDefinitionsQuery
    );

    const filteredComponentNames = useMemo(() => getWorkflowComponentNames(workflow), [workflow]);

    const openIntegrationInstanceConfigurationWorkflowSheet = useIntegrationInstanceConfigurationWorkflowSheetStore(
        (state) => state.openIntegrationInstanceConfigurationWorkflowSheet
    );

    const [showEditDialog, setShowEditDialog] = useState(false);

    const [, copyToClipboard] = useCopyToClipboard();
    const queryClient = useQueryClient();

    const enableIntegrationInstanceWorkflowMutation = useEnableIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(integrationInstance.connectedUserId!),
            });
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(integrationInstance.id!),
            });
        },
    });

    const lastExecutionDate = integrationInstanceWorkflow?.lastExecutionDate;

    const staticWebhookUrl = integrationInstanceWorkflow?.staticWebhookUrl;

    const handleWorkflowClick = () => {
        openIntegrationInstanceConfigurationWorkflowSheet({
            connectedUserId: integrationInstance.connectedUserId,
            integrationInstanceConfigurationId: integrationInstanceConfiguration.id!,
            integrationInstanceConfigurationName: integrationInstanceConfiguration.name,
            integrationVersion: integrationInstanceConfiguration.integrationVersion,
            workflow,
        });
    };

    const handleCopyStaticWebhookUrlClick = async () => {
        if (staticWebhookUrl) {
            await copyToClipboard(staticWebhookUrl);

            toast('Webhook URL copied to clipboard');
        }
    };

    const handleCopyWorkflowUuidClick = async () => {
        if (workflow.workflowUuid) {
            await copyToClipboard(workflow.workflowUuid);

            toast('Workflow UUID copied to clipboard');
        }
    };

    return (
        <li key={workflow.id}>
            <div
                className="flex min-h-12 cursor-pointer items-center justify-between gap-x-4 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
                onClick={(event) => {
                    if (isCollapsibleRowClick(event)) {
                        handleWorkflowClick();
                    }
                }}
                role="presentation"
            >
                <div className="flex min-w-0 flex-1 items-center gap-2">
                    <div className="flex w-80 shrink-0 items-center gap-2 pr-1">
                        <button
                            className="min-w-0 cursor-pointer truncate text-left text-sm font-semibold"
                            onClick={handleWorkflowClick}
                            type="button"
                        >
                            {workflow.label}
                        </button>

                        {mcpTool && <McpToolBadge />}
                    </div>

                    <WorkflowTriggerAndComponentsRow
                        className="hidden sm:flex"
                        filteredComponentNames={filteredComponentNames}
                        fitToWidth
                        workflow={workflow}
                        workflowComponentDefinitions={workflowComponentDefinitions}
                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                    />
                </div>

                <div className="flex shrink-0 items-center gap-x-4">
                    <Tooltip>
                        <TooltipTrigger className="text-xs whitespace-nowrap text-content-neutral-secondary">
                            {lastExecutionDate
                                ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                : 'No executions'}
                        </TooltipTrigger>

                        <TooltipContent side="left">Last Execution Date</TooltipContent>
                    </Tooltip>

                    <div className="min-w-[36px]">
                        {staticWebhookUrl && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        aria-label="Copy webhook URL"
                                        disabled={!integrationInstanceWorkflow?.enabled}
                                        icon={<ClipboardIcon />}
                                        onClick={handleCopyStaticWebhookUrlClick}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>
                            </Tooltip>
                        )}
                    </div>

                    <div className="flex items-center gap-x-1">
                        <div className="relative flex items-center">
                            {enableIntegrationInstanceWorkflowMutation.isPending && (
                                <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                            )}

                            <Switch
                                checked={integrationInstanceWorkflow?.enabled}
                                disabled={!integrationInstanceConfigurationWorkflow?.enabled}
                                onCheckedChange={(value) => {
                                    enableIntegrationInstanceWorkflowMutation.mutate({
                                        enable: value,
                                        id: integrationInstance.id!,
                                        workflowId: workflow.id!,
                                    });
                                }}
                            />
                        </div>

                        <ConnectedUserSheetDeleteDropdownMenu
                            onCopyWorkflowUuidClick={workflow.workflowUuid ? handleCopyWorkflowUuidClick : undefined}
                            onEditClick={() => setShowEditDialog(true)}
                        />
                    </div>
                </div>
            </div>

            {showEditDialog && (
                <ConnectedUserIntegrationWorkflowEditDialog
                    integrationInstance={integrationInstance}
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    integrationInstanceConfigurationWorkflow={integrationInstanceConfigurationWorkflow}
                    integrationInstanceWorkflow={integrationInstanceWorkflow}
                    onClose={() => setShowEditDialog(false)}
                    workflow={workflow}
                />
            )}
        </li>
    );
};

export default ConnectedUserSheetPanelIntegrationWorkflowListItem;
