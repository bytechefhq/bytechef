import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import SyncSourceStatusBadge from '@/shared/components/SyncSourceStatusBadge';
import {AUTHORITIES} from '@/shared/constants';
import {ContextStoreSource, useRefreshContextStoreSourceMutation} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {formatDistanceToNow} from 'date-fns';
import {RefreshCwIcon} from 'lucide-react';
import {Link} from 'react-router-dom';
import {toast} from 'sonner';

type SourceDetailFieldsType = Pick<
    ContextStoreSource,
    | 'cadence'
    | 'entityName'
    | 'id'
    | 'idField'
    | 'indexedFields'
    | 'lastSyncRunAt'
    | 'name'
    | 'sourceClusterElementName'
    | 'sourceComponentName'
    | 'sourceComponentVersion'
    | 'status'
    | 'workflowId'
>;

type ContextStoreSourceDetailDialogPropsType = {
    onOpenChange: (open: boolean) => void;
    open: boolean;
    source: SourceDetailFieldsType | null;
};

/**
 * Read-only summary dialog for a single Context Store source. Replaces the standalone
 * /automation/context-stores/:storeId/sources/:sourceId page so the list page stays in view while inspecting a row.
 */
const ContextStoreSourceDetailDialog = ({onOpenChange, open, source}: ContextStoreSourceDetailDialogPropsType) => {
    const account = useAuthenticationStore((state) => state.account);

    const queryClient = useQueryClient();

    const refreshMutation = useRefreshContextStoreSourceMutation({
        onError: (mutationError: unknown) => {
            const message = mutationError instanceof Error ? mutationError.message : 'Failed to trigger sync';

            toast.error(message);
        },
        onSuccess: (data) => {
            toast.success(`Sync triggered (job ${data.refreshContextStoreSource})`);

            queryClient.invalidateQueries({queryKey: ['contextStoreSource']});
            queryClient.invalidateQueries({queryKey: ['contextStoreSources']});
        },
    });

    const isAdmin = account?.authorities?.includes(AUTHORITIES.ADMIN) ?? false;

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>{source?.name ?? 'Context Store Source'}</DialogTitle>

                    <DialogDescription>Summary of this source's sync configuration.</DialogDescription>
                </DialogHeader>

                {source && (
                    <fieldset className="space-y-4 border-0 p-0">
                        <fieldset className="grid grid-cols-2 gap-4 rounded-md border border-border bg-card p-4 lg:grid-cols-3">
                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">Status</p>

                                <SyncSourceStatusBadge status={source.status} />
                            </fieldset>

                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">Cadence</p>

                                <p className="text-sm">{source.cadence}</p>
                            </fieldset>

                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">Component</p>

                                <p className="text-sm">
                                    {source.sourceComponentName} v{source.sourceComponentVersion}
                                </p>
                            </fieldset>

                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">Reader</p>

                                <p className="text-sm">{source.sourceClusterElementName || '—'}</p>
                            </fieldset>

                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">Last Sync</p>

                                <p className="text-sm">
                                    {source.lastSyncRunAt
                                        ? `${formatDistanceToNow(new Date(Number(source.lastSyncRunAt)))} ago`
                                        : 'Never'}
                                </p>
                            </fieldset>

                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">Workflow</p>

                                {source.workflowId ? (
                                    <Link
                                        className="text-sm text-content-brand-primary hover:underline"
                                        to={`/automation/executions?workflowId=${source.workflowId}`}
                                    >
                                        View execution history
                                    </Link>
                                ) : (
                                    <p className="text-sm text-muted-foreground">—</p>
                                )}
                            </fieldset>

                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">Entity</p>

                                <p className="text-sm">{source.entityName}</p>
                            </fieldset>

                            <fieldset className="border-0 p-0">
                                <p className="text-xs font-semibold text-muted-foreground uppercase">ID Field</p>

                                <p className="text-sm">{source.idField}</p>
                            </fieldset>
                        </fieldset>

                        <fieldset className="rounded-md border border-border bg-card p-4">
                            <p className="mb-2 text-sm font-semibold">Indexed Fields</p>

                            <pre className="overflow-x-auto rounded bg-muted p-2 text-xs">
                                {JSON.stringify(source.indexedFields ?? {}, null, 2)}
                            </pre>
                        </fieldset>
                    </fieldset>
                )}

                <DialogFooter>
                    {source && isAdmin && (
                        <Button
                            disabled={refreshMutation.isPending}
                            icon={<RefreshCwIcon />}
                            label="Refresh now"
                            onClick={() => refreshMutation.mutate({id: source.id})}
                            type="button"
                        />
                    )}

                    <Button label="Close" onClick={() => onOpenChange(false)} type="button" variant="outline" />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ContextStoreSourceDetailDialog;
