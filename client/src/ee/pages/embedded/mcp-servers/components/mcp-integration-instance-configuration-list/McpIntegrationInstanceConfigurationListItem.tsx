import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ChevronDownIcon, ChevronRightIcon, ComponentIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import McpIntegrationInstanceConfigurationWorkflowDialog from '../McpIntegrationInstanceConfigurationWorkflowDialog';
import McpIntegrationInstanceConfigurationListItemDropdownMenu from './McpIntegrationInstanceConfigurationListItemDropdownMenu';
import McpIntegrationInstanceConfigurationWorkflowList from './McpIntegrationInstanceConfigurationWorkflowList';
import {McpIntegrationInstanceConfigurationItemType} from './hooks/useMcpIntegrationInstanceConfigurationList';
import useMcpIntegrationInstanceConfigurationListItem from './hooks/useMcpIntegrationInstanceConfigurationListItem';

interface McpIntegrationInstanceConfigurationListItemProps {
    mcpIntegrationInstanceConfiguration: McpIntegrationInstanceConfigurationItemType;
}

const McpIntegrationInstanceConfigurationListItem = ({
    mcpIntegrationInstanceConfiguration,
}: McpIntegrationInstanceConfigurationListItemProps) => {
    const [expanded, setExpanded] = useState(true);

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
        <>
            <Collapsible className="group rounded-md border border-border" onOpenChange={setExpanded} open={expanded}>
                <div className="flex items-center gap-2.5 px-3 py-2.5">
                    <CollapsibleTrigger asChild>
                        <button
                            aria-label={expanded ? 'Collapse integration' : 'Expand integration'}
                            className="shrink-0 text-muted-foreground hover:text-foreground"
                            type="button"
                        >
                            {expanded ? (
                                <ChevronDownIcon className="size-4" />
                            ) : (
                                <ChevronRightIcon className="size-4" />
                            )}
                        </button>
                    </CollapsibleTrigger>

                    {componentDefinition?.icon ? (
                        <InlineSVG className="size-6 shrink-0" src={componentDefinition.icon} />
                    ) : (
                        <ComponentIcon className="size-6 shrink-0 text-content-neutral-secondary" />
                    )}

                    <span className="min-w-0 flex-1 truncate text-sm font-medium">
                        {mcpIntegrationInstanceConfiguration.integrationInstanceConfigurationName ||
                            `Integration ${mcpIntegrationInstanceConfiguration.integrationInstanceConfigurationId}`}
                    </span>

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

                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
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

                    <McpIntegrationInstanceConfigurationListItemDropdownMenu
                        mcpIntegrationInstanceConfiguration={mcpIntegrationInstanceConfiguration}
                        onEditWorkflowsClick={() => setShowEditWorkflowsDialog(true)}
                        onUpdateIntegrationVersionClick={() => setShowUpdateIntegrationVersionDialog(true)}
                    />
                </div>

                <CollapsibleContent>
                    <div className="border-t border-border px-3 py-2 pl-10">
                        <McpIntegrationInstanceConfigurationWorkflowList
                            componentName={mcpIntegrationInstanceConfiguration.integration?.componentName || ''}
                            mcpIntegrationInstanceConfigurationWorkflows={
                                mcpIntegrationInstanceConfiguration.mcpIntegrationInstanceConfigurationWorkflows
                            }
                        />
                    </div>
                </CollapsibleContent>
            </Collapsible>

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
        </>
    );
};

export default McpIntegrationInstanceConfigurationListItem;
