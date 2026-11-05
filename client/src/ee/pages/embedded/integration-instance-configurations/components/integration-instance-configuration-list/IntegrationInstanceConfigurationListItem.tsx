import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    CONFIGURATIONS_LOAD_FAILED_REASON,
    NO_OTHER_CONFIGURATION_REASON,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/moveConnectedUsersReasons';
import IntegrationInstanceConfigurationListItemDropdownMenu from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationListItemDropdownMenu';
import {useIntegrationInstanceConfigurationsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationsEnabledStore';
import {IntegrationInstanceConfiguration, IntegrationStatus, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {
    useDeleteIntegrationInstanceConfigurationMutation,
    useEnableIntegrationInstanceConfigurationMutation,
    useUpdateIntegrationInstanceConfigurationTagsMutation,
} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationTagKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {
    IntegrationInstanceConfigurationKeys,
    useGetIntegrationInstanceConfigurationsQuery,
} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationVersionsQuery} from '@/ee/shared/queries/embedded/integrationVersions.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import TagList from '@/shared/components/TagList';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, Settings2Icon} from 'lucide-react';
import {useCallback, useMemo, useRef, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import IntegrationInstanceConfigurationDialog from '../integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import IntegrationInstanceConfigurationMoveUsersDialog from './IntegrationInstanceConfigurationMoveUsersDialog';

interface IntegrationInstanceConfigurationListItemProps {
    integrationInstanceConfiguration: IntegrationInstanceConfiguration;
    mcpWorkflowIds?: Set<string>;
    remainingTags?: Tag[];
    showIntegrationIcon?: boolean;
    showTags?: boolean;
}

const IntegrationInstanceConfigurationListItem = ({
    integrationInstanceConfiguration,
    mcpWorkflowIds,
    remainingTags,
    showIntegrationIcon = true,
    showTags = true,
}: IntegrationInstanceConfigurationListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showMoveUsersDialog, setShowMoveUsersDialog] = useState(false);
    const [showUpdateIntegrationVersionDialog, setShowUpdateIntegrationVersionDialog] = useState(false);
    const setIntegrationInstanceConfigurationEnabled = useIntegrationInstanceConfigurationsEnabledStore(
        ({setIntegrationInstanceConfigurationEnabled}) => setIntegrationInstanceConfigurationEnabled
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const workflowsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const {data: integrationVersions} = useGetIntegrationVersionsQuery(
        integrationInstanceConfiguration.integrationId!,
        integrationInstanceConfiguration.integrationId != null
    );

    const workflowCount = integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows?.length ?? 0;

    const lastPublishedIntegrationVersion = (integrationVersions ?? [])
        .filter((integrationVersion) => integrationVersion.status === IntegrationStatus.Published)
        .reduce((lastVersion, integrationVersion) => Math.max(lastVersion, integrationVersion.version ?? 0), 0);

    const newerVersionAvailable =
        integrationInstanceConfiguration.integrationVersion != null &&
        lastPublishedIntegrationVersion > integrationInstanceConfiguration.integrationVersion;

    const {captureIntegrationInstanceConfigurationEnabled} = useAnalytics();

    const queryClient = useQueryClient();

    const deleteIntegrationInstanceConfigurationMutation = useDeleteIntegrationInstanceConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationTagKeys.integrationInstanceConfigurationTags,
            });
            void queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });
        },
    });

    const updateIntegrationInstanceConfigurationTagsMutation = useUpdateIntegrationInstanceConfigurationTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationTagKeys.integrationInstanceConfigurationTags,
            });
        },
    });

    const enableIntegrationInstanceConfigurationMutation = useEnableIntegrationInstanceConfigurationMutation({
        onSuccess: (_, variables) => {
            if (variables.enable) {
                captureIntegrationInstanceConfigurationEnabled();
            }

            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
            });
        },
    });

    const {data: integrationConfigurations, isError: integrationConfigurationsError} =
        useGetIntegrationInstanceConfigurationsQuery(
            {environmentId: currentEnvironmentId, integrationId: integrationInstanceConfiguration.integrationId},
            integrationInstanceConfiguration.integrationId != null
        );

    const moveTargetConfigurations = useMemo(
        () =>
            (integrationConfigurations ?? []).filter(
                (otherConfiguration) => otherConfiguration.id !== integrationInstanceConfiguration.id
            ),
        [integrationConfigurations, integrationInstanceConfiguration.id]
    );

    const moveUsersDisabledReason = useMemo(() => {
        if (integrationConfigurationsError) {
            return CONFIGURATIONS_LOAD_FAILED_REASON;
        }

        return integrationConfigurations && moveTargetConfigurations.length === 0
            ? NO_OTHER_CONFIGURATION_REASON
            : undefined;
    }, [integrationConfigurations, integrationConfigurationsError, moveTargetConfigurations.length]);

    const handleIntegrationInstanceConfigurationListItemClick = useCallback((event: React.MouseEvent) => {
        const target = event.target as HTMLElement;

        const interactiveSelectors = [
            '[data-interactive]',
            '[role="menuitem"]',
            '[role="menu"]',
            '[data-radix-dropdown-menu-trigger]',
            '[data-radix-collapsible-trigger]',
            'button',
            'input',
            'svg',
        ].join(', ');

        if (target.closest(interactiveSelectors)) {
            return;
        }

        if (workflowsCollapsibleTriggerRef.current?.contains(target)) {
            return;
        }

        workflowsCollapsibleTriggerRef.current?.click();
    }, []);

    const handleOnCheckedChange = (value: boolean) => {
        enableIntegrationInstanceConfigurationMutation.mutate(
            {
                enable: value,
                id: integrationInstanceConfiguration.id!,
            },
            {
                onSuccess: () => {
                    setIntegrationInstanceConfigurationEnabled(
                        integrationInstanceConfiguration.id!,
                        !integrationInstanceConfiguration.enabled
                    );
                    integrationInstanceConfiguration!.enabled = !integrationInstanceConfiguration.enabled;
                },
            }
        );
    };

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                onClick={(event) => handleIntegrationInstanceConfigurationListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-3 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex min-h-8 items-center justify-between">
                            <div className="flex w-full items-center gap-2">
                                <div className="flex items-center gap-2">
                                    {showIntegrationIcon && integrationInstanceConfiguration?.integration?.icon ? (
                                        <InlineSVG
                                            className="size-6 shrink-0"
                                            src={integrationInstanceConfiguration?.integration.icon}
                                        />
                                    ) : (
                                        <Settings2Icon
                                            aria-hidden="true"
                                            className="size-6 shrink-0 text-content-neutral-secondary"
                                        />
                                    )}

                                    <span className="text-base font-semibold text-content-neutral-primary">
                                        {integrationInstanceConfiguration?.name}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div className="mt-2 min-h-7 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                {integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows && (
                                    <CollapsibleTrigger
                                        className="group mr-4 flex text-xs font-semibold text-muted-foreground"
                                        disabled={workflowCount === 0}
                                        ref={workflowsCollapsibleTriggerRef}
                                    >
                                        <span className="mr-1">
                                            {workflowCount === 1 ? `1 workflow` : `${workflowCount} workflows`}
                                        </span>

                                        {workflowCount > 1 && (
                                            <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                        )}
                                    </CollapsibleTrigger>
                                )}

                                <div onClick={(event) => event.preventDefault()}>
                                    {showTags && integrationInstanceConfiguration.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={integrationInstanceConfiguration.id!}
                                            remainingTags={remainingTags}
                                            tags={integrationInstanceConfiguration.tags}
                                            updateTagsMutation={updateIntegrationInstanceConfigurationTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        {newerVersionAvailable && <Badge label="Newer version available" styleType="warning-outline" />}

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`V${integrationInstanceConfiguration.integrationVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>The integration version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="flex min-h-8 items-center">
                                {enableIntegrationInstanceConfigurationMutation.isPending && <LoadingIcon />}

                                <Switch
                                    checked={integrationInstanceConfiguration.enabled}
                                    disabled={enableIntegrationInstanceConfigurationMutation.isPending}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary">
                                    {integrationInstanceConfiguration.lastModifiedDate ? (
                                        <span className="text-xs">
                                            {`Modified at ${integrationInstanceConfiguration.lastModifiedDate?.toLocaleDateString()} ${integrationInstanceConfiguration.lastModifiedDate?.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No executions</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent side="left">Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <IntegrationInstanceConfigurationListItemDropdownMenu
                            moveUsersDisabled={!integrationConfigurations}
                            moveUsersDisabledReason={moveUsersDisabledReason}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                            onMoveUsersClick={() => setShowMoveUsersDialog(true)}
                            onUpdateIntegrationVersionClick={() => setShowUpdateIntegrationVersionDialog(true)}
                        />
                    </div>
                </div>
            </div>

            {showDeleteDialog && (
                <AlertDialog
                    description="This action cannot be undone. This will permanently delete the instance configuration. The integration and its workflows are not affected."
                    isPending={deleteIntegrationInstanceConfigurationMutation.isPending}
                    onCancel={() => setShowDeleteDialog(false)}
                    onConfirm={() => {
                        if (integrationInstanceConfiguration.id) {
                            deleteIntegrationInstanceConfigurationMutation.mutate(integrationInstanceConfiguration.id);
                        }
                    }}
                    open
                />
            )}

            {showEditDialog && (
                <IntegrationInstanceConfigurationDialog
                    excludeWorkflowIds={mcpWorkflowIds}
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    onClose={() => setShowEditDialog(false)}
                />
            )}

            {showMoveUsersDialog && (
                <IntegrationInstanceConfigurationMoveUsersDialog
                    configuration={integrationInstanceConfiguration}
                    onClose={() => setShowMoveUsersDialog(false)}
                    targetConfigurations={moveTargetConfigurations}
                />
            )}

            {showUpdateIntegrationVersionDialog && (
                <IntegrationInstanceConfigurationDialog
                    excludeWorkflowIds={mcpWorkflowIds}
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    onClose={() => setShowUpdateIntegrationVersionDialog(false)}
                    updateIntegrationVersion={true}
                />
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationListItem;
