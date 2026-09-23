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
import {
    ConnectedUserMcpServer,
    useDeleteConnectedUserMcpServerMutation,
    useEnableConnectedUserMcpServerMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';
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

    const queryClient = useQueryClient();

    const deleteConnectedUserMcpServerMutation = useDeleteConnectedUserMcpServerMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});

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

    const lastModifiedDate = mcpServer.lastModifiedDate ? new Date(Date.parse(mcpServer.lastModifiedDate)) : undefined;

    return (
        <>
            <Collapsible className="mb-2 rounded border border-border/50" key={mcpServer.id}>
                <div className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-destructive-foreground">
                    <CollapsibleTrigger className="flex-1 py-3">
                        <div className="flex flex-col items-start justify-center gap-y-2">
                            <div
                                className={twMerge(
                                    'flex min-h-8 items-center text-base font-semibold',
                                    !mcpServer.enabled && 'text-muted-foreground'
                                )}
                            >
                                {mcpServer.name}
                            </div>

                            <div className="flex min-h-7 items-center gap-4 text-xs font-semibold text-muted-foreground">
                                {toolCount > 0 && (
                                    <span>{toolCount === 1 ? '1 component tool' : `${toolCount} component tools`}</span>
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
                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="relative flex min-h-8 items-center">
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

                            {lastModifiedDate && (
                                <Tooltip>
                                    <TooltipTrigger className="flex min-h-7 items-center text-xs text-muted-foreground">
                                        {`Updated ${lastModifiedDate.toLocaleDateString()} ${lastModifiedDate.toLocaleTimeString()}`}
                                    </TooltipTrigger>

                                    <TooltipContent>Last Modified Date</TooltipContent>
                                </Tooltip>
                            )}
                        </div>

                        {workflowCount > 0 ? (
                            <div aria-hidden="true" className="size-9 shrink-0" />
                        ) : (
                            <ConnectedUserSheetDeleteDropdownMenu onDeleteClick={() => setShowDeleteDialog(true)} />
                        )}
                    </div>
                </div>

                <CollapsibleContent>
                    {toolCount > 0 && (
                        <div className="flex w-full flex-col gap-y-2 py-3">
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
                        <div className="flex w-full flex-col gap-y-2 py-3">
                            <h3 className="flex justify-start px-3 text-sm font-semibold text-muted-foreground uppercase">
                                Workflow Tools
                            </h3>

                            <div className="flex flex-col gap-2 px-3">
                                {workflowsByComponentName.map((group) => (
                                    <ConnectedUserMcpServerComponentGroup
                                        componentName={group.componentName}
                                        credentialStatus={getCredentialStatus(group.items[0].integrationInstanceId)}
                                        key={`${group.componentName}_${group.version}`}
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
