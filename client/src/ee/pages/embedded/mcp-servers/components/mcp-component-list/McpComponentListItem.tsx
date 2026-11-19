import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useMcpComponentListItem from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentListItem';
import McpComponentCollapsible from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentCollapsible';
import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import McpComponentToolList from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolList';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';

const McpComponentListItem = ({mcpComponent, mcpServer}: {mcpComponent: McpComponent; mcpServer: McpServer}) => {
    const {componentDefinition, integrationName, integrationUnavailable, setShowEditDialog, showEditDialog} =
        useMcpComponentListItem(mcpComponent.componentName, mcpComponent.componentVersion, mcpComponent.integrationId);

    return (
        <McpComponentCollapsible
            badges={
                mcpComponent.integrationId ? (
                    !integrationName &&
                    integrationUnavailable && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    className="relative z-10"
                                    label="Integration unavailable"
                                    styleType="warning-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>
                                The integration of this component is no longer published or configured. Edit this
                                component to choose another integration.
                            </TooltipContent>
                        </Tooltip>
                    )
                ) : (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Badge
                                className="relative z-10"
                                label="No integration"
                                styleType="warning-filled"
                                weight="semibold"
                            />
                        </TooltipTrigger>

                        <TooltipContent>
                            Edit this component to choose the integration whose connection its tools use.
                        </TooltipContent>
                    </Tooltip>
                )
            }
            dialog={
                <McpComponentDialog
                    integrationUnavailable={integrationUnavailable}
                    key={String(integrationUnavailable)}
                    mcpComponent={mcpComponent}
                    mcpServerId={mcpServer.id}
                    onOpenChange={setShowEditDialog}
                    open={showEditDialog}
                />
            }
            dropdownMenu={
                <McpComponentListItemDropdownMenu
                    embedded
                    mcpComponent={mcpComponent}
                    onEditClick={() => setShowEditDialog(true)}
                />
            }
            iconSrc={componentDefinition?.icon}
            mcpComponent={mcpComponent}
        >
            <McpComponentToolList
                componentName={mcpComponent.componentName}
                componentVersion={mcpComponent.componentVersion}
                connectionId={mcpComponent.connectionId}
                embedded
                mcpTools={mcpComponent.mcpTools}
                renderEditDialog={(open, onOpenChange) => (
                    <McpComponentDialog
                        mcpComponent={mcpComponent}
                        mcpServerId={mcpServer.id!}
                        onOpenChange={onOpenChange}
                        open={open}
                    />
                )}
            />
        </McpComponentCollapsible>
    );
};

export default McpComponentListItem;
