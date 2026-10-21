import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AppApiCollectionBindingDialog from '@/pages/automation/apps/components/AppApiCollectionBindingDialog';
import {
    App,
    AppApiCollectionBinding,
    useAppApiCollectionsQuery,
    useUnbindAppApiCollectionMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

interface AppApiCollectionsTabProps {
    app: App;
    canManage: boolean;
}

const AppApiCollectionsTab = ({app, canManage}: AppApiCollectionsTabProps) => {
    const [bindingToUnbind, setBindingToUnbind] = useState<
        Pick<AppApiCollectionBinding, 'apiCollectionId' | 'name'> | undefined
    >();

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAppApiCollectionsQuery({appId: app.id});

    const unbindAppApiCollectionMutation = useUnbindAppApiCollectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appApiCollections']});

            setBindingToUnbind(undefined);
        },
    });

    const appApiCollections = useMemo(() => data?.appApiCollections ?? [], [data?.appApiCollections]);

    const boundApiCollectionIds = useMemo(
        () => appApiCollections.map((appApiCollection) => appApiCollection.apiCollectionId),
        [appApiCollections]
    );

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <p className="text-sm text-content-neutral-secondary">
                    The API collections whose endpoints this app serves with its secret key.
                </p>

                {canManage && (
                    <AppApiCollectionBindingDialog
                        app={app}
                        boundApiCollectionIds={boundApiCollectionIds}
                        triggerNode={<Button label="Bind API Collection" />}
                    />
                )}
            </div>

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appApiCollections.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>

                                <TableHead>Context path</TableHead>

                                <TableHead>Version</TableHead>

                                <TableHead>Project</TableHead>

                                <TableHead />
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {appApiCollections.map((appApiCollection) => (
                                <TableRow key={appApiCollection.id}>
                                    <TableCell className="font-medium">
                                        <span className="flex items-center gap-2">
                                            {appApiCollection.name}

                                            {!appApiCollection.available && (
                                                <Badge
                                                    label="Unavailable"
                                                    styleType="warning-outline"
                                                    title="This collection, its deployment or its project is gone or disabled, so the app cannot serve its endpoints."
                                                />
                                            )}
                                        </span>
                                    </TableCell>

                                    <TableCell>
                                        {appApiCollection.contextPath && `/${appApiCollection.contextPath}`}
                                    </TableCell>

                                    <TableCell>{appApiCollection.collectionVersion || ''}</TableCell>

                                    <TableCell>{appApiCollection.projectName}</TableCell>

                                    <TableCell className="text-right">
                                        {canManage && (
                                            <Button
                                                aria-label={`Unbind ${appApiCollection.name}`}
                                                label="Unbind"
                                                onClick={() => setBindingToUnbind(appApiCollection)}
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
                        No API collections are bound to this app yet.
                    </p>
                )}
            </PageLoader>

            <AlertDialog
                confirmLabel="Unbind"
                description={`Unbind "${bindingToUnbind?.name}"? The app can no longer serve its endpoints.`}
                isPending={unbindAppApiCollectionMutation.isPending}
                onCancel={() => setBindingToUnbind(undefined)}
                onConfirm={() => {
                    if (bindingToUnbind) {
                        unbindAppApiCollectionMutation.mutate({
                            apiCollectionId: bindingToUnbind.apiCollectionId,
                            appId: app.id,
                        });
                    }
                }}
                open={bindingToUnbind !== undefined}
                title="Unbind API collection?"
            />
        </div>
    );
};

export default AppApiCollectionsTab;
