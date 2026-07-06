import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import useMcpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentListItem';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';
import {ChevronDownIcon, ChevronRightIcon, ComponentIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';
import McpComponentToolList from './McpComponentToolList';

const McpComponentListItem = ({mcpComponent, mcpServer}: {mcpComponent: McpComponent; mcpServer: McpServer}) => {
    const {componentDefinition, setShowEditDialog, showEditDialog} = useMcpComponentListItem(
        mcpComponent.componentName,
        mcpComponent.componentVersion
    );

    const [expanded, setExpanded] = useState(false);

    return (
        <Collapsible className="group rounded-md border border-border/50" onOpenChange={setExpanded} open={expanded}>
            <div className="flex items-center gap-2.5 px-3 py-2.5">
                <CollapsibleTrigger asChild>
                    <button
                        aria-label={expanded ? 'Hide tools' : 'Show tools'}
                        className="shrink-0 text-muted-foreground hover:text-foreground"
                        type="button"
                    >
                        {expanded ? <ChevronDownIcon className="size-4" /> : <ChevronRightIcon className="size-4" />}
                    </button>
                </CollapsibleTrigger>

                {componentDefinition?.icon ? (
                    <InlineSVG className="size-6 shrink-0" src={componentDefinition.icon} />
                ) : (
                    <ComponentIcon className="size-6 shrink-0 text-content-neutral-secondary" />
                )}

                <CollapsibleTrigger asChild>
                    <button className="flex min-w-0 flex-1 cursor-pointer items-center gap-2 text-left" type="button">
                        <span className="truncate text-sm font-medium">
                            {mcpComponent.title || mcpComponent.componentName}
                        </span>
                    </button>
                </CollapsibleTrigger>

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

                <Tooltip>
                    <TooltipTrigger className="flex items-center text-xs text-content-neutral-secondary">
                        {mcpComponent.lastModifiedDate
                            ? `Modified at ${new Date(mcpComponent.lastModifiedDate).toLocaleDateString()} ${new Date(mcpComponent.lastModifiedDate).toLocaleTimeString()}`
                            : '-'}
                    </TooltipTrigger>

                    <TooltipContent>Last Updated Date</TooltipContent>
                </Tooltip>

                <McpComponentListItemDropdownMenu
                    mcpComponent={mcpComponent}
                    onEditClick={() => setShowEditDialog(true)}
                />
            </div>

            <CollapsibleContent>
                <div className="border-t border-border/50 px-3 py-3 pl-10">
                    <McpComponentToolList
                        componentName={mcpComponent.componentName}
                        componentVersion={mcpComponent.componentVersion}
                        connectionId={mcpComponent.connectionId}
                        mcpComponent={mcpComponent}
                        mcpServerId={mcpServer.id!}
                        mcpTools={mcpComponent.mcpTools}
                    />
                </div>
            </CollapsibleContent>

            <McpComponentDialog
                mcpComponent={mcpComponent}
                mcpServerId={mcpServer.id}
                onOpenChange={setShowEditDialog}
                open={showEditDialog}
            />
        </Collapsible>
    );
};

export default McpComponentListItem;
