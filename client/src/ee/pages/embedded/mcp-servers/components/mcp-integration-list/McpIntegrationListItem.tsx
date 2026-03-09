import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import {McpIntegration} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';

import McpIntegrationWorkflowDialog from '../McpIntegrationWorkflowDialog';
import McpIntegrationListItemDropdownMenu from './McpIntegrationListItemDropdownMenu';
import useMcpIntegrationListItem from './hooks/useMcpIntegrationListItem';

interface McpIntegrationListItemProps {
    mcpIntegration: McpIntegration;
}

const McpIntegrationListItem = ({mcpIntegration}: McpIntegrationListItemProps) => {
    const {
        handleOnIntegrationInstanceConfigurationDialogClose,
        integrationInstanceConfiguration,
        setShowEditWorkflowsDialog,
        setShowUpdateIntegrationVersionDialog,
        showEditWorkflowsDialog,
        showUpdateIntegrationVersionDialog,
    } = useMcpIntegrationListItem(mcpIntegration);

    return (
        <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
            <div className="flex flex-1 items-center py-1">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="flex w-full items-center gap-x-2">
                            <WorkflowIcon className="size-4 flex-none text-gray-500" />

                            <span className="mr-2 text-base font-semibold">
                                {mcpIntegration.integration?.name ||
                                    `Integration ${mcpIntegration.integrationInstanceConfigurationId}`}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    {mcpIntegration.integrationVersion && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`v${mcpIntegration.integrationVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Integration Version</TooltipContent>
                        </Tooltip>
                    )}

                    <div className="flex min-w-52 flex-col items-end gap-y-4">
                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {mcpIntegration.lastModifiedDate ? (
                                    <span className="text-xs">
                                        {`Modified at ${new Date(mcpIntegration.lastModifiedDate).toLocaleDateString()} ${new Date(mcpIntegration.lastModifiedDate).toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Last Updated Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <McpIntegrationListItemDropdownMenu
                        mcpIntegration={mcpIntegration}
                        onEditWorkflowsClick={() => setShowEditWorkflowsDialog(true)}
                        onUpdateIntegrationVersionClick={() => setShowUpdateIntegrationVersionDialog(true)}
                    />
                </div>
            </div>

            {showEditWorkflowsDialog && (
                <McpIntegrationWorkflowDialog
                    mcpIntegration={mcpIntegration}
                    onClose={() => setShowEditWorkflowsDialog(false)}
                />
            )}

            {showUpdateIntegrationVersionDialog && (
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    onClose={handleOnIntegrationInstanceConfigurationDialogClose}
                    updateIntegrationVersion={true}
                />
            )}
        </div>
    );
};

export default McpIntegrationListItem;
