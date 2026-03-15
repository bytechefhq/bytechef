import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import {McpIntegrationInstanceConfiguration} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import McpIntegrationInstanceConfigurationWorkflowDialog from '../McpIntegrationInstanceConfigurationWorkflowDialog';
import McpIntegrationInstanceConfigurationListItemDropdownMenu from './McpIntegrationInstanceConfigurationListItemDropdownMenu';
import useMcpIntegrationInstanceConfigurationListItem from './hooks/useMcpIntegrationInstanceConfigurationListItem';

interface McpIntegrationInstanceConfigurationListItemProps {
    mcpIntegrationInstanceConfiguration: McpIntegrationInstanceConfiguration;
}

const McpIntegrationInstanceConfigurationListItem = ({
    mcpIntegrationInstanceConfiguration,
}: McpIntegrationInstanceConfigurationListItemProps) => {
    const {
        handleOnIntegrationInstanceConfigurationDialogClose,
        integrationInstanceConfiguration,
        mcpWorkflowUuids,
        setShowEditWorkflowsDialog,
        setShowUpdateIntegrationVersionDialog,
        showEditWorkflowsDialog,
        showUpdateIntegrationVersionDialog,
    } = useMcpIntegrationInstanceConfigurationListItem(mcpIntegrationInstanceConfiguration);

    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: mcpIntegrationInstanceConfiguration.integration?.componentName || '',
            componentVersion: 1,
        },
        !!mcpIntegrationInstanceConfiguration.integration?.componentName
    );

    return (
        <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
            <div className="flex flex-1 items-center py-1">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="flex w-full items-center gap-x-2">
                            {componentDefinition?.icon ? (
                                <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                            ) : (
                                <ComponentIcon className="size-4 flex-none text-gray-500" />
                            )}

                            <span className="mr-2 text-base font-semibold">
                                {mcpIntegrationInstanceConfiguration.integrationInstanceConfigurationName ||
                                    `Integration ${mcpIntegrationInstanceConfiguration.integrationInstanceConfigurationId}`}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    {mcpIntegrationInstanceConfiguration.integrationVersion && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`v${mcpIntegrationInstanceConfiguration.integrationVersion}`}
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
                                {mcpIntegrationInstanceConfiguration.lastModifiedDate ? (
                                    <span className="text-xs">
                                        {`Modified at ${new Date(mcpIntegrationInstanceConfiguration.lastModifiedDate).toLocaleDateString()} ${new Date(mcpIntegrationInstanceConfiguration.lastModifiedDate).toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Last Updated Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <McpIntegrationInstanceConfigurationListItemDropdownMenu
                        mcpIntegrationInstanceConfiguration={mcpIntegrationInstanceConfiguration}
                        onEditWorkflowsClick={() => setShowEditWorkflowsDialog(true)}
                        onUpdateIntegrationVersionClick={() => setShowUpdateIntegrationVersionDialog(true)}
                    />
                </div>
            </div>

            {showEditWorkflowsDialog && (
                <McpIntegrationInstanceConfigurationWorkflowDialog
                    mcpIntegrationInstanceConfiguration={mcpIntegrationInstanceConfiguration}
                    onClose={() => setShowEditWorkflowsDialog(false)}
                />
            )}

            {showUpdateIntegrationVersionDialog && (
                <IntegrationInstanceConfigurationDialog
                    filterWorkflowUuids={mcpWorkflowUuids}
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    onClose={handleOnIntegrationInstanceConfigurationDialogClose}
                    updateIntegrationVersion={true}
                />
            )}
        </div>
    );
};

export default McpIntegrationInstanceConfigurationListItem;
