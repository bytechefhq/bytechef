import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {
    ConnectedUserProjectsQuery,
    useDeleteConnectedUserProjectWorkflowMutation,
    useEnableConnectedUserProjectWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

export type ConnectedUserProjectWorkflowType =
    ConnectedUserProjectsQuery['connectedUserProjects'][number]['connectedUserProjectWorkflows'][number];

const ConnectedUserProjectWorkflowListItem = ({
    connectedUserProjectWorkflow,
}: {
    connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType;
}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const canDeleteConnectedUser = useHasEmbeddedScope('CONNECTED_USER_DELETE');
    const canManageConnectedUser = useHasEmbeddedScope('CONNECTED_USER_MANAGE');

    const queryClient = useQueryClient();

    const deleteConnectedUserProjectWorkflowMutation = useDeleteConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});

            setShowDeleteDialog(false);
        },
    });

    const enableConnectedUserProjectWorkflowMutation = useEnableConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
        },
    });

    const lastExecutionDate = connectedUserProjectWorkflow.lastExecutionDate
        ? new Date(Date.parse(connectedUserProjectWorkflow.lastExecutionDate))
        : undefined;

    const isDraft = connectedUserProjectWorkflow.workflowVersion == null;

    const {staticWebhookUrl} = connectedUserProjectWorkflow;

    const handleCopyStaticWebhookUrlClick = async () => {
        if (staticWebhookUrl) {
            await copyToClipboard(staticWebhookUrl);

            toast('Webhook URL copied to clipboard');
        }
    };

    return (
        <>
            <li className="mb-2 flex items-center justify-between rounded border border-border/50 p-3 hover:bg-surface-neutral-primary-hover">
                <div className="flex min-h-8 min-w-0 flex-1 flex-col justify-center gap-y-1">
                    <span className="truncate text-sm font-semibold">
                        {connectedUserProjectWorkflow.workflow.label}
                    </span>

                    {staticWebhookUrl && (
                        <div className="flex min-w-0 items-center gap-x-1">
                            <span
                                className="truncate font-mono text-xs text-content-neutral-secondary"
                                title={staticWebhookUrl}
                            >
                                {staticWebhookUrl}
                            </span>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        aria-label="Copy webhook URL"
                                        icon={<ClipboardIcon />}
                                        onClick={handleCopyStaticWebhookUrlClick}
                                        size="iconXs"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>
                            </Tooltip>
                        </div>
                    )}
                </div>

                <div className="flex items-center gap-x-2">
                    <div className="flex items-center gap-x-4 text-content-neutral-secondary">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={isDraft ? 'V1 DRAFT' : `V${connectedUserProjectWorkflow.workflowVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>The workflow version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="relative flex min-h-8 items-center">
                                {enableConnectedUserProjectWorkflowMutation.isPending && (
                                    <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                                )}

                                <Switch
                                    checked={connectedUserProjectWorkflow.enabled}
                                    disabled={!canManageConnectedUser}
                                    onCheckedChange={(value) => {
                                        enableConnectedUserProjectWorkflowMutation.mutate({
                                            enable: value,
                                            id: connectedUserProjectWorkflow.id,
                                        });
                                    }}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-7 items-center text-sm">
                                    {lastExecutionDate ? (
                                        <span className="text-xs">
                                            {`Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No executions</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent side="left">Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>
                    </div>

                    {canDeleteConnectedUser && (
                        <ConnectedUserSheetDeleteDropdownMenu onDeleteClick={() => setShowDeleteDialog(true)} />
                    )}
                </div>
            </li>

            <AlertDialog
                isPending={deleteConnectedUserProjectWorkflowMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() =>
                    deleteConnectedUserProjectWorkflowMutation.mutate({id: connectedUserProjectWorkflow.id})
                }
                open={showDeleteDialog}
            />
        </>
    );
};

export default ConnectedUserProjectWorkflowListItem;
