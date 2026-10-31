import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useMcpComponentListItem from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentListItem';
import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';
import {ChevronDownIcon, ChevronRightIcon, ComponentIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';
import McpComponentToolList from './McpComponentToolList';

const McpComponentListItem = ({mcpComponent, mcpServer}: {mcpComponent: McpComponent; mcpServer: McpServer}) => {
    const {componentDefinition, integrationName, integrationUnavailable, setShowEditDialog, showEditDialog} =
        useMcpComponentListItem(mcpComponent.componentName, mcpComponent.componentVersion, mcpComponent.integrationId);

    const [expanded, setExpanded] = useState(false);

    return (
        <Collapsible className="group rounded-md border border-border/50" onOpenChange={setExpanded} open={expanded}>
            <div className="relative flex items-center gap-2.5 rounded-md px-3 py-2.5 hover:bg-surface-neutral-primary-hover">
                <CollapsibleTrigger asChild>
                    <button
                        aria-label={expanded ? 'Hide tools' : 'Show tools'}
                        className="shrink-0 text-muted-foreground after:absolute after:inset-0 hover:text-foreground"
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

                {mcpComponent.integrationId ? (
                    integrationName ? (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    className="relative z-10"
                                    label={integrationName}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Integration whose connection these tools use</TooltipContent>
                        </Tooltip>
                    ) : (
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
                )}

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Badge
                            className="relative z-10"
                            label={`v${mcpComponent.componentVersion}`}
                            styleType="outline-outline"
                            weight="semibold"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Component Version</TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger className="relative z-10 flex min-w-56 items-center justify-end text-xs text-content-neutral-secondary tabular-nums">
                        {mcpComponent.lastModifiedDate
                            ? `Modified at ${new Date(mcpComponent.lastModifiedDate).toLocaleDateString()} ${new Date(mcpComponent.lastModifiedDate).toLocaleTimeString()}`
                            : '-'}
                    </TooltipTrigger>

                    <TooltipContent>Last Updated Date</TooltipContent>
                </Tooltip>

                <McpComponentListItemDropdownMenu
                    embedded
                    mcpComponent={mcpComponent}
                    onEditClick={() => setShowEditDialog(true)}
                />
            </div>

            <CollapsibleContent>
                <div className="border-t border-border/50 px-3 py-2 pl-10">
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
                integrationUnavailable={integrationUnavailable}
                key={String(integrationUnavailable)}
                mcpComponent={mcpComponent}
                mcpServerId={mcpServer.id}
                onOpenChange={setShowEditDialog}
                open={showEditDialog}
            />
        </Collapsible>
    );
};

export default McpComponentListItem;
