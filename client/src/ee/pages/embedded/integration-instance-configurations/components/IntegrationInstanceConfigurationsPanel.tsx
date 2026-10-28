import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import LoadingIcon from '@/components/LoadingIcon';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import IntegrationInstanceConfigurationList from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationList';
import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationInstanceConfigurationTagsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {useGetIntegrationInstanceConfigurationsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {AlertTriangleIcon, PlusIcon, SendIcon, Settings2Icon} from 'lucide-react';
import {useMemo} from 'react';

interface IntegrationInstanceConfigurationsPanelProps {
    enabled: boolean;
    integration: Integration;
}

const IntegrationInstanceConfigurationsPanel = ({
    enabled,
    integration,
}: IntegrationInstanceConfigurationsPanelProps) => {
    const {
        data: integrationInstanceConfigurations,
        error: integrationInstanceConfigurationsError,
        isLoading: integrationInstanceConfigurationsIsLoading,
        refetch: refetchIntegrationInstanceConfigurations,
    } = useGetIntegrationInstanceConfigurationsQuery(
        {
            environmentId: DEVELOPMENT_ENVIRONMENT,
            integrationId: integration.id,
        },
        enabled && !!integration.id
    );

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {
            actionDefinitions: true,
            triggerDefinitions: true,
        },
        enabled
    );

    const {data: tags} = useGetIntegrationInstanceConfigurationTagsQuery();

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const sortedIntegrationInstanceConfigurations = useMemo(
        () =>
            [...(integrationInstanceConfigurations ?? [])].sort(
                (integrationInstanceConfiguration, otherIntegrationInstanceConfiguration) =>
                    (integrationInstanceConfiguration.name ?? '').localeCompare(
                        otherIntegrationInstanceConfiguration.name ?? ''
                    )
            ),
        [integrationInstanceConfigurations]
    );

    const isPublished = !!(integration.lastPublishedDate && integration.lastIntegrationVersion);

    const integrationInstanceConfigurationsLoadFailed =
        !!integrationInstanceConfigurationsError && integrationInstanceConfigurations === undefined;

    return (
        <>
            {integrationInstanceConfigurationsIsLoading && (
                <div className="flex items-center justify-center py-6">
                    <LoadingIcon />
                </div>
            )}

            {integrationInstanceConfigurationsLoadFailed && (
                <EmptyState
                    actions={
                        <Button
                            label="Retry"
                            onClick={() => void refetchIntegrationInstanceConfigurations()}
                            variant="outline"
                        />
                    }
                    description="The instance configurations of this integration could not be loaded. Please try again."
                    icon={<AlertTriangleIcon />}
                    title="Failed to load configurations"
                />
            )}

            {!integrationInstanceConfigurationsIsLoading &&
                !integrationInstanceConfigurationsLoadFailed &&
                sortedIntegrationInstanceConfigurations.length === 0 &&
                (isPublished ? (
                    <EmptyState
                        actions={
                            <IntegrationInstanceConfigurationDialog
                                integrationInstanceConfiguration={
                                    {
                                        environmentId: DEVELOPMENT_ENVIRONMENT,
                                        integrationId: integration.id,
                                    } as IntegrationInstanceConfiguration
                                }
                                triggerNode={<Button icon={<PlusIcon />} label="Create Configuration" />}
                            />
                        }
                        description="Create an instance configuration to run this integration's workflows here."
                        icon={<Settings2Icon />}
                        title="No configurations in Development"
                    />
                ) : (
                    <EmptyState
                        description="Instance configurations run a published version of an integration. Publish this integration to create its first version."
                        icon={<SendIcon />}
                        title="Publish the integration to configure it"
                    />
                ))}

            {sortedIntegrationInstanceConfigurations.length > 0 && (
                <WorkflowReadOnlyProvider
                    value={{
                        useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                    }}
                >
                    <IntegrationInstanceConfigurationList
                        componentDefinitions={componentDefinitions}
                        expandAll
                        integration={integration}
                        integrationInstanceConfigurations={sortedIntegrationInstanceConfigurations}
                        tags={tags ?? []}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                    />
                </WorkflowReadOnlyProvider>
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationsPanel;
