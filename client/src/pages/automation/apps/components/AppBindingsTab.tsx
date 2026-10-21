import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AppBindingDialog from '@/pages/automation/apps/components/AppBindingDialog';
import {AppBinding, useAppBindingsQuery, useUnbindAppConnectionMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EditIcon, EllipsisVerticalIcon, LinkIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface AppBindingsTabProps {
    appInstanceId: string;
    canManage: boolean;
}

const AppBindingsTab = ({appInstanceId, canManage}: AppBindingsTabProps) => {
    const [bindingKeyToUnbind, setBindingKeyToUnbind] = useState<string | undefined>();
    const [bindingToEdit, setBindingToEdit] = useState<AppBinding | undefined>();

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAppBindingsQuery({appInstanceId});

    const unbindAppConnectionMutation = useUnbindAppConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appBindings']});

            setBindingKeyToUnbind(undefined);
        },
    });

    const appBindings = data?.appBindings ?? [];

    const empty = !isLoading && !error && appBindings.length === 0;

    const bindConnectionButton = canManage && (
        <AppBindingDialog appInstanceId={appInstanceId} triggerNode={<Button label="Bind Connection" />} />
    );

    return (
        <div className="flex flex-1 flex-col space-y-4">
            {!empty && (
                <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <p className="text-sm text-content-neutral-secondary">
                        The connections this app can use. The app never sees the credentials.
                    </p>

                    {bindConnectionButton}
                </div>
            )}

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appBindings.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Binding key</TableHead>

                                <TableHead>Connection</TableHead>

                                <TableHead className="max-sm:hidden">Component</TableHead>

                                <TableHead className="max-md:hidden">Actions</TableHead>

                                <TableHead className="max-md:hidden">Tools</TableHead>

                                <TableHead className="max-md:hidden">Proxy methods</TableHead>

                                <TableHead />
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {appBindings.map((appBinding) => (
                                <TableRow key={appBinding.id}>
                                    <TableCell className="font-medium">{appBinding.bindingKey}</TableCell>

                                    <TableCell>{appBinding.connectionName}</TableCell>

                                    <TableCell className="max-sm:hidden">
                                        {appBinding.componentName}

                                        {appBinding.componentVersion == null && (
                                            <span
                                                className="ml-2 text-content-warning-primary"
                                                title="This connection's component cannot be found, so calls through this binding fail."
                                            >
                                                Unavailable
                                            </span>
                                        )}
                                    </TableCell>

                                    <TableCell className="max-md:hidden">{appBinding.actionNames.length}</TableCell>

                                    <TableCell className="max-md:hidden">{appBinding.toolNames.length}</TableCell>

                                    <TableCell className="max-md:hidden">
                                        {appBinding.proxyEnabled
                                            ? appBinding.proxyAllowedMethods.join(', ') || '-'
                                            : 'Off'}
                                    </TableCell>

                                    <TableCell className="text-right">
                                        {canManage && (
                                            <DropdownMenu>
                                                <DropdownMenuTrigger asChild>
                                                    <Button
                                                        aria-label={`${appBinding.bindingKey} actions`}
                                                        icon={<EllipsisVerticalIcon />}
                                                        size="icon"
                                                        variant="ghost"
                                                    />
                                                </DropdownMenuTrigger>

                                                <DropdownMenuContent align="end" className="p-0">
                                                    <DropdownMenuItem
                                                        className="dropdown-menu-item"
                                                        onClick={() => setBindingToEdit(appBinding)}
                                                    >
                                                        <EditIcon /> Edit
                                                    </DropdownMenuItem>

                                                    <DropdownMenuSeparator className="m-0" />

                                                    <DropdownMenuItem
                                                        className="dropdown-menu-item-destructive"
                                                        onClick={() => setBindingKeyToUnbind(appBinding.bindingKey)}
                                                        variant="destructive"
                                                    >
                                                        <Trash2Icon /> Unbind
                                                    </DropdownMenuItem>
                                                </DropdownMenuContent>
                                            </DropdownMenu>
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                ) : (
                    <EmptyState
                        actions={bindConnectionButton}
                        description="Bind a connection so the app can run its actions and tools, or call its API through ByteChef. The app never sees the credentials."
                        icon={<LinkIcon />}
                        title="No connections bound"
                    />
                )}
            </PageLoader>

            {bindingToEdit && (
                <AppBindingDialog
                    appInstanceId={appInstanceId}
                    binding={{
                        actionNames: bindingToEdit.actionNames,
                        bindingKey: bindingToEdit.bindingKey,
                        connectionId: bindingToEdit.connectionId,
                        proxyAllowedMethods: bindingToEdit.proxyAllowedMethods,
                        proxyEnabled: bindingToEdit.proxyEnabled,
                        toolNames: bindingToEdit.toolNames,
                    }}
                    key={bindingToEdit.bindingKey}
                    onClose={() => setBindingToEdit(undefined)}
                />
            )}

            <AlertDialog
                confirmLabel="Unbind"
                description={`Unbind "${bindingKeyToUnbind}"? The app can no longer use this connection.`}
                isPending={unbindAppConnectionMutation.isPending}
                onCancel={() => setBindingKeyToUnbind(undefined)}
                onConfirm={() => {
                    if (bindingKeyToUnbind) {
                        unbindAppConnectionMutation.mutate({appInstanceId, bindingKey: bindingKeyToUnbind});
                    }
                }}
                open={bindingKeyToUnbind !== undefined}
                title="Unbind connection?"
            />
        </div>
    );
};

export default AppBindingsTab;
