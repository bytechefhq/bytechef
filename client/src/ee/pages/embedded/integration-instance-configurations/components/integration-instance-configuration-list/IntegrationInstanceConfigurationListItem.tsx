import LoadingIcon from '@/components/LoadingIcon';
import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationListItemAlertDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationListItemAlertDialog';
import IntegrationInstanceConfigurationListItemDropdownMenu from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationListItemDropdownMenu';
import {useIntegrationInstanceConfigurationsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationsEnabledStore';
import {IntegrationInstanceConfiguration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {
    useDeleteIntegrationInstanceConfigurationMutation,
    useEnableIntegrationInstanceConfigurationMutation,
    useUpdateIntegrationInstanceConfigurationTagsMutation,
} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationTagKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {IntegrationInstanceConfigurationKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import TagList from '@/shared/components/TagList';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import IntegrationInstanceConfigurationDialog from '../integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';

interface IntegrationInstanceConfigurationListItemProps {
    integrationInstanceConfiguration: IntegrationInstanceConfiguration;
    remainingTags?: Tag[];
}

const IntegrationInstanceConfigurationListItem = ({
    integrationInstanceConfiguration,
    remainingTags,
}: IntegrationInstanceConfigurationListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showUpdateIntegrationVersionDialog, setShowUpdateIntegrationVersionDialog] = useState(false);
    const setIntegrationInstanceConfigurationEnabled = useIntegrationInstanceConfigurationsEnabledStore(
        ({setIntegrationInstanceConfigurationEnabled}) => setIntegrationInstanceConfigurationEnabled
    );

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
        onSuccess: () => {
            captureIntegrationInstanceConfigurationEnabled();

            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
            });
        },
    });

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
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="flex w-full items-center gap-2">
                                <div className="flex items-center gap-1">
                                    {integrationInstanceConfiguration?.integration?.icon && (
                                        <InlineSVG
                                            className="size-5 flex-none"
                                            src={integrationInstanceConfiguration?.integration.icon}
                                        />
                                    )}

                                    <span className="text-base font-semibold text-gray-900">
                                        {integrationInstanceConfiguration?.name}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                {integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows && (
                                    <CollapsibleTrigger
                                        className="group mr-4 flex text-xs font-semibold text-muted-foreground"
                                        disabled={
                                            integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows
                                                .length === 0
                                        }
                                    >
                                        <span className="mr-1">
                                            {integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows
                                                ?.length === 1
                                                ? `1 workflow`
                                                : `${integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows?.length} workflows`}
                                        </span>

                                        {integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows
                                            .length > 1 && (
                                            <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                        )}
                                    </CollapsibleTrigger>
                                )}

                                <div onClick={(event) => event.preventDefault()}>
                                    {integrationInstanceConfiguration.tags && (
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
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge variant="secondary">
                                    <Badge variant="secondary">
                                        V{integrationInstanceConfiguration.integrationVersion}
                                    </Badge>
                                </Badge>
                            </TooltipTrigger>

                            <TooltipContent>The integration version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <div className="flex items-center">
                                {enableIntegrationInstanceConfigurationMutation.isPending && <LoadingIcon />}

                                <Switch
                                    checked={integrationInstanceConfiguration.enabled}
                                    disabled={enableIntegrationInstanceConfigurationMutation.isPending}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {integrationInstanceConfiguration.lastModifiedDate ? (
                                        <span className="text-xs">
                                            {`Modified at ${integrationInstanceConfiguration.lastModifiedDate?.toLocaleDateString()} ${integrationInstanceConfiguration.lastModifiedDate?.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No executions</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <IntegrationInstanceConfigurationListItemDropdownMenu
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                            onUpdateIntegrationVersionClick={() => setShowUpdateIntegrationVersionDialog(true)}
                        />
                    </div>
                </div>
            </div>

            {showDeleteDialog && (
                <IntegrationInstanceConfigurationListItemAlertDialog
                    isPending={deleteIntegrationInstanceConfigurationMutation.isPending}
                    onCancelClick={() => setShowDeleteDialog(false)}
                    onDeleteClick={() => {
                        if (integrationInstanceConfiguration.id) {
                            deleteIntegrationInstanceConfigurationMutation.mutate(integrationInstanceConfiguration.id);
                        }
                    }}
                />
            )}

            {showEditDialog && (
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    onClose={() => setShowEditDialog(false)}
                />
            )}

            {showUpdateIntegrationVersionDialog && (
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    onClose={() => setShowUpdateIntegrationVersionDialog(false)}
                    updateIntegrationVersion={true}
                />
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationListItem;
