import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AppBindingDialog from '@/pages/automation/apps/components/AppBindingDialog';
import {App, useAppBindingsQuery, useUnbindAppConnectionMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface AppBindingsTabProps {
    app: App;
    canManage: boolean;
}

const AppBindingsTab = ({app, canManage}: AppBindingsTabProps) => {
    const [bindingKeyToUnbind, setBindingKeyToUnbind] = useState<string | undefined>();

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAppBindingsQuery({appId: app.id});

    const unbindAppConnectionMutation = useUnbindAppConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appBindings']});

            setBindingKeyToUnbind(undefined);
        },
    });

    const appBindings = data?.appBindings ?? [];

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <p className="text-sm text-content-neutral-secondary">
                    The connections this app can use. The app never sees the credentials.
                </p>

                {canManage && <AppBindingDialog app={app} triggerNode={<Button label="Bind Connection" />} />}
            </div>

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appBindings.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Binding key</TableHead>

                                <TableHead>Connection</TableHead>

                                <TableHead>Component</TableHead>

                                <TableHead>Actions</TableHead>

                                <TableHead>Tools</TableHead>

                                <TableHead>Proxy methods</TableHead>

                                <TableHead />
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {appBindings.map((appBinding) => (
                                <TableRow key={appBinding.id}>
                                    <TableCell className="font-medium">{appBinding.bindingKey}</TableCell>

                                    <TableCell>{appBinding.connectionName}</TableCell>

                                    <TableCell>
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

                                    <TableCell>{appBinding.actionNames.length}</TableCell>

                                    <TableCell>{appBinding.toolNames.length}</TableCell>

                                    <TableCell>
                                        {appBinding.proxyEnabled
                                            ? appBinding.proxyAllowedMethods.join(', ') || '-'
                                            : 'Off'}
                                    </TableCell>

                                    <TableCell className="text-right">
                                        {canManage && (
                                            <Button
                                                aria-label={`Unbind ${appBinding.bindingKey}`}
                                                label="Unbind"
                                                onClick={() => setBindingKeyToUnbind(appBinding.bindingKey)}
                                                size="sm"
                                                variant="destructiveGhost"
                                            />
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                ) : (
                    <p className="py-8 text-center text-sm text-content-neutral-secondary">
                        No connections are bound to this app yet.
                    </p>
                )}
            </PageLoader>

            <AlertDialog
                confirmLabel="Unbind"
                description={`Unbind "${bindingKeyToUnbind}"? The app can no longer use this connection.`}
                isPending={unbindAppConnectionMutation.isPending}
                onCancel={() => setBindingKeyToUnbind(undefined)}
                onConfirm={() => {
                    if (bindingKeyToUnbind) {
                        unbindAppConnectionMutation.mutate({appId: app.id, bindingKey: bindingKeyToUnbind});
                    }
                }}
                open={bindingKeyToUnbind !== undefined}
                title="Unbind connection?"
            />
        </div>
    );
};

export default AppBindingsTab;
