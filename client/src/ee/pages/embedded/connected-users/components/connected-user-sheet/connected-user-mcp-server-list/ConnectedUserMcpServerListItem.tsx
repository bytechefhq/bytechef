import AlertDialog from '@/components/AlertDialog';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import ConnectedUserMcpServerComponentGroup from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerComponentGroup';
import ConnectedUserMcpServerListItemToolRow from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItemToolRow';
import ConnectedUserMcpServerListItemWorkflowRow from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItemWorkflowRow';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import isCollapsibleRowClick from '@/shared/components/mcp-server/utils/isCollapsibleRowClick';
import {
    ConnectedUserMcpServer,
    useDeleteConnectedUserMcpServerMutation,
    useEnableConnectedUserMcpServerMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {type MouseEvent, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const ConnectedUserMcpServerListItem = ({
    connectedUserId,
    connectedUserIntegrationInstances,
    mcpServer,
}: {
    connectedUserId: number;
    connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[];
    mcpServer: ConnectedUserMcpServer;
}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [open, setOpen] = useState(false);

    const queryClient = useQueryClient();

    const deleteConnectedUserMcpServerMutation = useDeleteConnectedUserMcpServerMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
            void queryClient.invalidateQueries({queryKey: ['provisionableConnectedUserMcpServers']});

            setShowDeleteDialog(false);
        },
    });

    const enableConnectedUserMcpServerMutation = useEnableConnectedUserMcpServerMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});
        },
    });

    const toolCount = mcpServer.tools.length;
    const workflowCount = mcpServer.workflows.length;
    const hasItems = toolCount > 0 || workflowCount > 0;

    const toolsByComponentName = useMemo(
        () => groupByComponent(mcpServer.tools, (tool) => tool.componentVersion),
        [mcpServer.tools]
    );

    const workflowsByComponentName = useMemo(
        () => groupByComponent(mcpServer.workflows, (workflow) => workflow.integrationVersion),
        [mcpServer.workflows]
    );

    const getCredentialStatus = (integrationInstanceId: string) =>
        connectedUserIntegrationInstances.find(
            (integrationInstance) => String(integrationInstance.id) === integrationInstanceId
        )?.credentialStatus;

    const lastExecutionDate = useMemo(() => getLastExecutionDate(mcpServer.workflows), [mcpServer.workflows]);

    const handleRowClick = (event: MouseEvent<HTMLDivElement>) => {
        if (hasItems && isCollapsibleRowClick(event)) {
            setOpen((previousOpen) => !previousOpen);
        }
    };

    return (
        <>
            <Collapsible
                className="mb-2 rounded border border-border/50"
                key={mcpServer.id}
                onOpenChange={setOpen}
                open={open}
            >
                <div
                    className="flex cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                    onClick={handleRowClick}
                    role="presentation"
                >
                    <ListItemExpandButton disabled={!hasItems} label="Toggle MCP server" />

                    <CollapsibleTrigger className="flex-1 py-3" disabled={!hasItems}>
                        <div className="flex flex-col items-start justify-center gap-y-2">
                            <div
                                className={twMerge(
                                    'flex min-h-6 items-center text-base font-semibold',
                                    !mcpServer.enabled && 'text-muted-foreground'
                                )}
                            >
                                {mcpServer.name}
                            </div>

                            <div className="flex min-h-6 items-center text-xs font-semibold text-muted-foreground">
                                {toolCount > 0 && (
                                    <span>{toolCount === 1 ? '1 component tool' : `${toolCount} component tools`}</span>
                                )}

                                {toolCount > 0 && workflowCount > 0 && (
                                    <span aria-hidden="true" className="mx-2">
                                        -
                                    </span>
                                )}

                                {workflowCount > 0 && (
                                    <span>
                                        {workflowCount === 1 ? '1 workflow tool' : `${workflowCount} workflow tools`}
                                    </span>
                                )}
                            </div>
                        </div>
                    </CollapsibleTrigger>

                    <div className="flex items-center gap-x-2">
                        <div className="flex w-72 flex-col items-end gap-y-2">
                            <div className="relative flex min-h-6 items-center">
                                {enableConnectedUserMcpServerMutation.isPending && (
                                    <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                                )}

                                <Switch
                                    checked={mcpServer.enabled}
                                    onCheckedChange={(value) => {
                                        enableConnectedUserMcpServerMutation.mutate({
                                            connectedUserId: connectedUserId.toString(),
                                            enable: value,
                                            mcpServerId: mcpServer.id,
                                        });
                                    }}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-6 items-center text-xs text-content-neutral-secondary">
                                    {lastExecutionDate
                                        ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                        : 'No executions'}
                                </TooltipTrigger>

                                <TooltipContent side="left">Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <ConnectedUserSheetDeleteDropdownMenu
                            deleteDisabledReason={
                                workflowCount > 0
                                    ? "Workflow tools come with this user's integrations, so this server can't be deleted. Switch it off instead."
                                    : undefined
                            }
                            onDeleteClick={() => setShowDeleteDialog(true)}
                        />
                    </div>
                </div>

                <CollapsibleContent className="ml-8">
                    {toolCount > 0 && (
                        <div className="flex w-full flex-col gap-y-2 py-3 first:pt-1">
                            <h3 className="flex justify-start px-3 text-sm font-semibold text-muted-foreground uppercase">
                                Component Tools
                            </h3>

                            <div className="flex flex-col gap-2 px-3">
                                {toolsByComponentName.map((group) => (
                                    <ConnectedUserMcpServerComponentGroup
                                        componentName={group.componentName}
                                        credentialStatus={getCredentialStatus(group.items[0].integrationInstanceId)}
                                        key={`${group.componentName}_${group.version}`}
                                        version={group.version}
                                        versionKind="component"
                                    >
                                        {group.items.map((tool) => (
                                            <ConnectedUserMcpServerListItemToolRow key={tool.id} tool={tool} />
                                        ))}
                                    </ConnectedUserMcpServerComponentGroup>
                                ))}
                            </div>
                        </div>
                    )}

                    {workflowCount > 0 && (
                        <div className="flex w-full flex-col gap-y-2 py-3 first:pt-1">
                            <h3 className="flex justify-start px-3 text-sm font-semibold text-muted-foreground uppercase">
                                Workflow Tools
                            </h3>

                            <div className="flex flex-col gap-2 px-3">
                                {workflowsByComponentName.map((group) => (
                                    <ConnectedUserMcpServerComponentGroup
                                        componentName={group.componentName}
                                        credentialStatus={getCredentialStatus(group.items[0].integrationInstanceId)}
                                        key={`${group.componentName}_${group.version}`}
                                        lastExecutionDate={getLastExecutionDate(group.items)}
                                        version={group.version}
                                        versionKind="integration"
                                    >
                                        {group.items.map((workflow) => (
                                            <ConnectedUserMcpServerListItemWorkflowRow
                                                connectedUserId={connectedUserId}
                                                key={`${workflow.integrationInstanceId}_${workflow.workflowId}`}
                                                workflow={workflow}
                                            />
                                        ))}
                                    </ConnectedUserMcpServerComponentGroup>
                                ))}
                            </div>
                        </div>
                    )}

                    {toolCount === 0 && workflowCount === 0 && (
                        <div className="px-3 py-3 text-sm text-muted-foreground">No tools enabled for this user.</div>
                    )}
                </CollapsibleContent>
            </Collapsible>

            <AlertDialog
                isPending={deleteConnectedUserMcpServerMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() =>
                    deleteConnectedUserMcpServerMutation.mutate({
                        connectedUserId: connectedUserId.toString(),
                        mcpServerId: mcpServer.id,
                    })
                }
                open={showDeleteDialog}
            />
        </>
    );
};

function getLastExecutionDate(workflows: Array<{lastExecutionDate?: string | null}>): Date | undefined {
    const executionTimes = workflows
        .map((workflow) => workflow.lastExecutionDate)
        .filter((executionDate): executionDate is string => executionDate != null)
        .map((executionDate) => Date.parse(executionDate));

    return executionTimes.length > 0 ? new Date(Math.max(...executionTimes)) : undefined;
}

interface ComponentGroupI<T> {
    componentName: string;
    items: T[];
    version: number;
}

function groupByComponent<T extends {componentName: string}>(
    items: T[],
    getVersion: (item: T) => number
): ComponentGroupI<T>[] {
    const groupsByKey = new Map<string, ComponentGroupI<T>>();

    for (const item of items) {
        const version = getVersion(item);
        const key = `${item.componentName}_${version}`;

        const group = groupsByKey.get(key) ?? {componentName: item.componentName, items: [], version};

        group.items.push(item);

        groupsByKey.set(key, group);
    }

    return [...groupsByKey.values()];
}

export default ConnectedUserMcpServerListItem;
