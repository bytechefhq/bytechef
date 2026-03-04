import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import useMcpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentListItem';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';

const McpComponentListItem = ({mcpComponent, mcpServer}: {mcpComponent: McpComponent; mcpServer: McpServer}) => {
    const {componentDefinition, setShowEditDialog, showEditDialog} = useMcpComponentListItem(
        mcpComponent.componentName,
        mcpComponent.componentVersion
    );

    return (
        <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
            <div className="flex flex-1 items-center py-1">
                <div className="flex flex-1 cursor-pointer items-center">
                    <div className="flex flex-1 items-center gap-x-2">
                        {componentDefinition?.icon ? (
                            <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                        ) : (
                            <ComponentIcon className="size-4 text-gray-500" />
                        )}

                        <span className="mr-2 text-base font-semibold">
                            {mcpComponent.title || mcpComponent.componentName}
                        </span>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`v${mcpComponent.componentVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Component Version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {mcpComponent.lastModifiedDate ? (
                                        <span className="text-xs">
                                            {`Modified at ${new Date(mcpComponent.lastModifiedDate).toLocaleDateString()} ${new Date(mcpComponent.lastModifiedDate).toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        '-'
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Updated Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <McpComponentListItemDropdownMenu
                            mcpComponent={mcpComponent}
                            onEditClick={() => setShowEditDialog(true)}
                        />
                    </div>
                </div>
            </div>

            <McpComponentDialog
                mcpComponent={mcpComponent}
                mcpServerId={mcpServer.id}
                onOpenChange={setShowEditDialog}
                open={showEditDialog}
            />
        </div>
    );
};

export default McpComponentListItem;
