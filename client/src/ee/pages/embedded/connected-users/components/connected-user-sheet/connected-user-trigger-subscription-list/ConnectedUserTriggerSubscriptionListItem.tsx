import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasic} from '@/ee/shared/middleware/embedded/configuration';
import {
    ConnectedUserTriggerSubscriptionsQuery,
    TriggerSubscriptionStatus,
    useDeleteTriggerSubscriptionMutation,
    useEnableTriggerSubscriptionMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, EllipsisVerticalIcon, PauseIcon, PlayIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

type TriggerSubscriptionType = ConnectedUserTriggerSubscriptionsQuery['connectedUserTriggerSubscriptions'][number];

type StatusBadgeStyleType = 'destructive-filled' | 'secondary-filled' | 'success-filled';

const STATUS_BADGE_STYLE_TYPES: Record<TriggerSubscriptionStatus, StatusBadgeStyleType> = {
    [TriggerSubscriptionStatus.ConnectionError]: 'destructive-filled',
    [TriggerSubscriptionStatus.Disabled]: 'secondary-filled',
    [TriggerSubscriptionStatus.Enabled]: 'success-filled',
    [TriggerSubscriptionStatus.Failed]: 'destructive-filled',
};

interface ConnectedUserTriggerSubscriptionListItemProps {
    componentDefinition?: ComponentDefinitionBasic;
    triggerSubscription: TriggerSubscriptionType;
}

const ConnectedUserTriggerSubscriptionListItem = ({
    componentDefinition,
    triggerSubscription,
}: ConnectedUserTriggerSubscriptionListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteTriggerSubscriptionMutation = useDeleteTriggerSubscriptionMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserTriggerSubscriptions']});

            setShowDeleteDialog(false);
        },
    });

    const enableTriggerSubscriptionMutation = useEnableTriggerSubscriptionMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserTriggerSubscriptions']});
        },
    });

    const enabled = triggerSubscription.status === TriggerSubscriptionStatus.Enabled;

    const lastEventDate = triggerSubscription.lastEventDate ? new Date(triggerSubscription.lastEventDate) : undefined;

    const statusBadge = (
        <Badge
            label={triggerSubscription.status}
            styleType={STATUS_BADGE_STYLE_TYPES[triggerSubscription.status]}
            weight="semibold"
        />
    );

    return (
        <>
            <li className="flex min-h-12 items-center justify-between gap-x-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
                <div className="flex min-w-0 flex-1 items-center gap-2.5">
                    {componentDefinition?.icon ? (
                        <InlineSVG className="size-5 shrink-0" src={componentDefinition.icon} />
                    ) : (
                        <ComponentIcon className="size-5 shrink-0 text-content-neutral-secondary" />
                    )}

                    <div className="flex min-w-0 flex-col gap-y-0.5">
                        <span className="truncate text-sm font-semibold" title={componentDefinition?.title}>
                            {triggerSubscription.triggerName}
                        </span>

                        {triggerSubscription.name && (
                            <span className="truncate text-xs text-content-neutral-secondary">
                                {triggerSubscription.name}
                            </span>
                        )}
                    </div>
                </div>

                <div className="flex items-center gap-x-4">
                    <div className="flex min-w-40 flex-col items-end gap-y-0.5 text-xs text-content-neutral-secondary">
                        <span>
                            {lastEventDate
                                ? `Last event at ${lastEventDate.toLocaleDateString()} ${lastEventDate.toLocaleTimeString()}`
                                : 'No events'}
                        </span>

                        {triggerSubscription.lastDeliveryStatus && (
                            <span>{triggerSubscription.lastDeliveryStatus}</span>
                        )}

                        {triggerSubscription.lastDeliveryError && (
                            <span className="max-w-56 truncate text-content-destructive-primary">
                                {triggerSubscription.lastDeliveryError}
                            </span>
                        )}
                    </div>

                    {triggerSubscription.errorMessage ? (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <span>{statusBadge}</span>
                            </TooltipTrigger>

                            <TooltipContent side="left">{triggerSubscription.errorMessage}</TooltipContent>
                        </Tooltip>
                    ) : (
                        statusBadge
                    )}

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                aria-label="More actions"
                                icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                                size="icon"
                                variant="ghost"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem
                                disabled={enableTriggerSubscriptionMutation.isPending}
                                icon={enabled ? <PauseIcon /> : <PlayIcon />}
                                label={enabled ? 'Disable' : 'Enable'}
                                onClick={() =>
                                    enableTriggerSubscriptionMutation.mutate({
                                        enable: !enabled,
                                        id: triggerSubscription.id,
                                    })
                                }
                            />

                            <DropdownMenuItem
                                icon={<Trash2Icon />}
                                label="Delete"
                                onClick={() => setShowDeleteDialog(true)}
                                variant="destructive"
                            />
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </li>

            <AlertDialog
                isPending={deleteTriggerSubscriptionMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => deleteTriggerSubscriptionMutation.mutate({id: triggerSubscription.id})}
                open={showDeleteDialog}
            />
        </>
    );
};

export default ConnectedUserTriggerSubscriptionListItem;
