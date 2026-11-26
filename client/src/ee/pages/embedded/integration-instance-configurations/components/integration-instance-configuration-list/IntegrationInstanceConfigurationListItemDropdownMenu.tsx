import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/Tooltip/Tooltip';
import {IntegrationInstanceConfigurationKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import EEVersion from '@/shared/edition/EEVersion';
import {PromotionResourceType, useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, RocketIcon, Trash2Icon, UsersIcon} from 'lucide-react';
import {Suspense, lazy, useState} from 'react';

const EnvironmentPromotionDialog = lazy(
    () => import('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog')
);

interface IntegrationInstanceConfigurationListItemDropdownMenuProps {
    integrationInstanceConfigurationId: number;
    integrationInstanceConfigurationName: string;
    onDeleteClick: () => void;
    onEditClick: () => void;
    moveUsersDisabled?: boolean;
    moveUsersDisabledReason?: string;
    onMoveUsersClick: () => void;
    onUpdateIntegrationVersionClick: () => void;
}

const IntegrationInstanceConfigurationListItemDropdownMenu = ({
    integrationInstanceConfigurationId,
    integrationInstanceConfigurationName,
    moveUsersDisabled,
    moveUsersDisabledReason,
    onDeleteClick,
    onEditClick,
    onMoveUsersClick,
    onUpdateIntegrationVersionClick,
}: IntegrationInstanceConfigurationListItemDropdownMenuProps) => {
    const [showPromotionDialog, setShowPromotionDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const environmentsQuery = useEnvironmentsQuery();

    const showPromoteToEnvironment = (environmentsQuery.data?.environments?.length ?? 0) >= 2;

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

                    <DropdownMenuItem
                        icon={<RefreshCcwIcon />}
                        label="Change Integration Version"
                        onClick={onUpdateIntegrationVersionClick}
                    />

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="block">
                                <DropdownMenuItem
                                    aria-label="Move Users"
                                    disabled={moveUsersDisabled || !!moveUsersDisabledReason}
                                    icon={<UsersIcon />}
                                    label="Move Users…"
                                    onClick={onMoveUsersClick}
                                />
                            </span>
                        </TooltipTrigger>

                        {moveUsersDisabledReason && (
                            <TooltipContent side="left">{moveUsersDisabledReason}</TooltipContent>
                        )}
                    </Tooltip>

                    {showPromoteToEnvironment && (
                        <EEVersion hidden={true}>
                            <DropdownMenuItem
                                icon={<RocketIcon />}
                                label="Promote to environment…"
                                onClick={() => setShowPromotionDialog(true)}
                            />
                        </EEVersion>
                    )}

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={onDeleteClick}
                        variant="destructive"
                    />
                </DropdownMenuContent>
            </DropdownMenu>

            {showPromotionDialog && (
                <EEVersion hidden={true}>
                    <Suspense fallback={null}>
                        <EnvironmentPromotionDialog
                            onClose={() => setShowPromotionDialog(false)}
                            onPromoted={() => {
                                void queryClient.invalidateQueries({
                                    queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
                                });
                            }}
                            resourceType={PromotionResourceType.IntegrationInstanceConfiguration}
                            sourceEnvironmentId={Number(currentEnvironmentId)}
                            sourceId={String(integrationInstanceConfigurationId)}
                            sourceName={integrationInstanceConfigurationName}
                            targetListPath="/embedded/configurations"
                        />
                    </Suspense>
                </EEVersion>
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationListItemDropdownMenu;
