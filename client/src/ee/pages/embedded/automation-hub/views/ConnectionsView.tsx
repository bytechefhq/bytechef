import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import LoadingDots from '@/components/LoadingDots';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {useDeleteHubConnectionMutation} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {useGetConnectionsQuery} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import HubConnectionDialog from '@/ee/pages/embedded/automation-hub/views/components/HubConnectionDialog';
import {
    Connection,
    ConnectionInUseError,
    ConnectionInUseErrorReasonEnum,
    ResponseError,
} from '@/ee/shared/middleware/embedded/public';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {EllipsisVerticalIcon, RefreshCwIcon, Trash2Icon} from 'lucide-react';
import {Fragment, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

interface DeleteErrorI {
    connectionId: number;
    message: string;
}

/**
 * The hub's second tab: list the connected user's own connections, reconnect (reauthorize) one in
 * place, or delete it. Deliberately narrow scope (spec §3, decision D5) — there is no standalone
 * "create connection" here; connections are created in context by the activation wizard and inside
 * the builder.
 */
const ConnectionsView = () => {
    const [deleteError, setDeleteError] = useState<DeleteErrorI>();
    const [deletingConnection, setDeletingConnection] = useState<Connection>();
    const [reconnectingConnection, setReconnectingConnection] = useState<Connection>();

    const {data: connections, error: connectionsError, isLoading: connectionsLoading} = useGetConnectionsQuery();
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const {isPending: deleteConnectionPending, mutate: deleteConnection} = useDeleteHubConnectionMutation();

    const componentInfoByName = useMemo(() => {
        const infoByName = new Map<string, {icon?: string; title?: string}>();

        for (const componentDefinition of componentDefinitions || []) {
            infoByName.set(componentDefinition.name, {
                icon: componentDefinition.icon,
                title: componentDefinition.title,
            });
        }

        return infoByName;
    }, [componentDefinitions]);

    const handleDeleteConfirm = () => {
        if (!deletingConnection) {
            return;
        }

        const connectionId = deletingConnection.id;

        deleteConnection(connectionId, {
            // Any delete failure gets an inline message next to its row, not just the global
            // toast — CONNECTION_IS_USED keeps its specific wording, everything else (a non-409
            // response, a body that isn't a ConnectionInUseError, a network failure) falls back to
            // a generic one rather than leaving the row with no feedback at all.
            onError: async (error: Error) => {
                const body =
                    error instanceof ResponseError
                        ? ((await error.response.json().catch(() => null)) as ConnectionInUseError | null)
                        : null;

                setDeleteError({
                    connectionId,
                    message:
                        body?.reason === ConnectionInUseErrorReasonEnum.ConnectionIsUsed
                            ? 'This connection is still used by an enabled automation.'
                            : 'Unable to delete this connection. Please try again.',
                });

                setDeletingConnection(undefined);
            },
            onSuccess: () => {
                setDeleteError(undefined);
                setDeletingConnection(undefined);
            },
        });
    };

    if (connectionsLoading) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="connections-view-loading">
                <LoadingDots />
            </div>
        );
    }

    return (
        <div className="flex size-full flex-col gap-8 overflow-y-auto p-6">
            <h1 className="text-lg font-semibold">Connections</h1>

            {connectionsError && (
                <Alert variant="destructive">
                    <AlertTitle>Unable to load connections</AlertTitle>

                    <AlertDescription>{connectionsError.message}</AlertDescription>
                </Alert>
            )}

            {!connectionsError &&
                (!connections?.length ? (
                    <p className="text-sm text-muted-foreground">You have not connected any accounts yet.</p>
                ) : (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>

                                <TableHead>App</TableHead>

                                <TableHead>Created</TableHead>

                                <TableHead />
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {connections.map((connection) => {
                                const componentInfo = connection.componentName
                                    ? componentInfoByName.get(connection.componentName)
                                    : undefined;

                                return (
                                    <Fragment key={connection.id}>
                                        <TableRow>
                                            <TableCell className="font-medium">{connection.name}</TableCell>

                                            <TableCell>
                                                <div className="flex items-center gap-2">
                                                    {componentInfo?.icon && (
                                                        <div className="flex size-6 shrink-0 items-center justify-center rounded-full border bg-background p-1">
                                                            <InlineSVG
                                                                className="size-3.5 flex-none"
                                                                src={componentInfo.icon}
                                                            />
                                                        </div>
                                                    )}

                                                    <span>{componentInfo?.title || connection.componentName}</span>
                                                </div>
                                            </TableCell>

                                            <TableCell>
                                                {connection.createdDate
                                                    ? connection.createdDate.toLocaleDateString()
                                                    : ''}
                                            </TableCell>

                                            <TableCell>
                                                <DropdownMenu>
                                                    <DropdownMenuTrigger asChild>
                                                        <Button
                                                            aria-label="Connection actions"
                                                            icon={<EllipsisVerticalIcon />}
                                                            size="icon"
                                                            variant="ghost"
                                                        />
                                                    </DropdownMenuTrigger>

                                                    <DropdownMenuContent align="end">
                                                        <DropdownMenuItem
                                                            onClick={() => setReconnectingConnection(connection)}
                                                        >
                                                            <RefreshCwIcon /> Reconnect
                                                        </DropdownMenuItem>

                                                        <DropdownMenuItem
                                                            onClick={() => setDeletingConnection(connection)}
                                                            variant="destructive"
                                                        >
                                                            <Trash2Icon /> Delete
                                                        </DropdownMenuItem>
                                                    </DropdownMenuContent>
                                                </DropdownMenu>
                                            </TableCell>
                                        </TableRow>

                                        {deleteError?.connectionId === connection.id && (
                                            <TableRow>
                                                <TableCell colSpan={4}>
                                                    <Alert variant="destructive">
                                                        <AlertDescription>{deleteError.message}</AlertDescription>
                                                    </Alert>
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </Fragment>
                                );
                            })}
                        </TableBody>
                    </Table>
                ))}

            <DeleteAlertDialog
                description={
                    deletingConnection
                        ? `This will remove "${deletingConnection.name}". This action cannot be undone.`
                        : undefined
                }
                isPending={deleteConnectionPending}
                onCancel={() => setDeletingConnection(undefined)}
                onDelete={handleDeleteConfirm}
                open={!!deletingConnection}
            />

            {reconnectingConnection?.componentName && (
                <HubConnectionDialog
                    componentName={reconnectingConnection.componentName}
                    existingConnectionId={reconnectingConnection.id}
                    existingConnectionVersion={reconnectingConnection.connectionVersion}
                    onClose={() => setReconnectingConnection(undefined)}
                />
            )}
        </div>
    );
};

export default ConnectionsView;
