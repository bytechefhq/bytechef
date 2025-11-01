import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import IntegrationInstanceConfigurationsFilterTitle from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationsFilterTitle';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import IntegrationInstanceConfigurationList from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationList';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationInstanceConfigurationTagsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {useGetIntegrationInstanceConfigurationsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ReadOnlyWorkflowSheet from '@/shared/components/read-only-workflow-editor/ReadOnlyWorkflowSheet';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Settings2Icon, TagIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

export enum Type {
    Integration,
    Tag,
    UnifiedAPI,
}

const IntegrationInstanceConfigurations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    const integrationId = searchParams.get('integrationId');
    const tagId = searchParams.get('tagId');

    const filterData: {id: number | string | undefined; type: Type} = {
        id: integrationId ? parseInt(integrationId) : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Integration,
    };

    const ff_743 = useFeatureFlagsStore()('ff-743');

    const {
        data: componentDefinitions,
        error: componentDefinitionsError,
        isLoading: componentDefinitionsLoading,
    } = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {
        data: integrations,
        error: integrationsError,
        isLoading: integrationsLoading,
    } = useGetIntegrationsQuery({
        includeAllFields: false,
        integrationInstanceConfigurations: true,
    });

    const {
        data: integrationInstanceConfigurations,
        error: integrationInstanceConfigurationsError,
        isLoading: integrationInstanceConfigurationsLoading,
    } = useGetIntegrationInstanceConfigurationsQuery({
        environmentId: currentEnvironmentId,
        integrationId: searchParams.get('integrationId') ? parseInt(searchParams.get('integrationId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const integrationInstanceConfigurationMap: Map<number, IntegrationInstanceConfiguration[]> = new Map<
        number,
        IntegrationInstanceConfiguration[]
    >();

    if (integrationInstanceConfigurations) {
        for (const integrationInstanceConfiguration of integrationInstanceConfigurations) {
            let currentIntegrationInstanceConfigurations: IntegrationInstanceConfiguration[];

            if (integrationInstanceConfiguration.integration) {
                if (integrationInstanceConfigurationMap.has(integrationInstanceConfiguration.integrationId!)) {
                    currentIntegrationInstanceConfigurations = integrationInstanceConfigurationMap.get(
                        integrationInstanceConfiguration.integrationId!
                    )!;
                } else {
                    currentIntegrationInstanceConfigurations = [];
                }

                currentIntegrationInstanceConfigurations.push(integrationInstanceConfiguration);

                integrationInstanceConfigurationMap.set(
                    integrationInstanceConfiguration.integrationId!,
                    currentIntegrationInstanceConfigurations
                );
            }
        }
    }

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetIntegrationInstanceConfigurationTagsQuery();

    return (
        <LayoutContainer
            header={
                integrationInstanceConfigurations &&
                integrationInstanceConfigurations?.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            integrationInstanceConfigurations &&
                            integrationInstanceConfigurations?.length > 0 && (
                                <IntegrationInstanceConfigurationDialog
                                    integrationInstanceConfiguration={
                                        {
                                            environmentId: currentEnvironmentId,
                                        } as IntegrationInstanceConfiguration
                                    }
                                    triggerNode={<Button label="New Instance Configuration" />}
                                />
                            )
                        }
                        title={
                            <IntegrationInstanceConfigurationsFilterTitle
                                filterData={filterData}
                                integrations={integrations}
                                tags={tags}
                            />
                        }
                    />
                )
            }
            leftSidebarBody={
                <>
                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        current: !filterData?.id && filterData.type === Type.Integration,
                                        name: 'All Integrations',
                                    }}
                                />

                                {componentDefinitions &&
                                    integrations &&
                                    integrations?.map((item) => (
                                        <LeftSidebarNavItem
                                            item={{
                                                current:
                                                    filterData?.id === item.id && filterData.type === Type.Integration,
                                                id: item.id,
                                                name:
                                                    componentDefinitions.find(
                                                        (componentDefinition) =>
                                                            componentDefinition.name === item.componentName!
                                                    )?.title ?? '',
                                            }}
                                            key={item.id}
                                            toLink={`?integrationId=${item.id}`}
                                        />
                                    ))}
                            </>
                        }
                        title="Integrations"
                    />

                    <LeftSidebarNav
                        body={
                            <>
                                {!tagsIsLoading &&
                                    (tags && !!tags.length ? (
                                        tags?.map((item) => (
                                            <LeftSidebarNavItem
                                                icon={<TagIcon className="mr-1 size-4" />}
                                                item={{
                                                    current: filterData?.id === item.id && filterData.type === Type.Tag,
                                                    id: item.id!,
                                                    name: item.name,
                                                }}
                                                key={item.id}
                                                toLink={`?tagId=${item.id}`}
                                            />
                                        ))
                                    ) : (
                                        <span className="px-3 text-xs">No defined tags.</span>
                                    ))}
                            </>
                        }
                        title="Tags"
                    />

                    {ff_743 && (
                        <LeftSidebarNav
                            body={
                                <>
                                    <LeftSidebarNavItem
                                        item={{
                                            current:
                                                filterData?.id === 'accounting' && filterData.type === Type.UnifiedAPI,
                                            id: 'accounting',
                                            name: 'Accounting',
                                        }}
                                        toLink="?unifiedApiCategory=accounting"
                                    />

                                    <LeftSidebarNavItem
                                        item={{
                                            current:
                                                filterData?.id === 'commerce' && filterData.type === Type.UnifiedAPI,
                                            id: 'commerce',
                                            name: 'Commerce',
                                        }}
                                        toLink="?unifiedApiCategory=commerce"
                                    />

                                    <LeftSidebarNavItem
                                        item={{
                                            current: filterData?.id === 'crm' && filterData.type === Type.UnifiedAPI,
                                            id: 'crm',
                                            name: 'CRM',
                                        }}
                                        toLink="?unifiedApiCategory=crm"
                                    />
                                </>
                            }
                            title="Unified API"
                        />
                    )}
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Instance Configurations" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[
                    componentDefinitionsError,
                    integrationsError,
                    integrationInstanceConfigurationsError,
                    tagsError,
                ]}
                loading={
                    componentDefinitionsLoading ||
                    integrationsLoading ||
                    integrationInstanceConfigurationsLoading ||
                    tagsIsLoading
                }
            >
                {componentDefinitions &&
                integrationInstanceConfigurations &&
                integrationInstanceConfigurations?.length > 0 ? (
                    <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
                        <WorkflowReadOnlyProvider
                            value={{
                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                            }}
                        >
                            {Array.from(integrationInstanceConfigurationMap.keys())?.map(
                                (integrationId) =>
                                    integrations &&
                                    tags && (
                                        <IntegrationInstanceConfigurationList
                                            integration={
                                                integrations.find(
                                                    (currentIntegration) => currentIntegration.id === integrationId
                                                )!
                                            }
                                            integrationInstanceConfigurations={
                                                integrationInstanceConfigurationMap.get(integrationId)!
                                            }
                                            key={integrationId}
                                            tags={tags}
                                        />
                                    )
                            )}

                            <ReadOnlyWorkflowSheet />
                        </WorkflowReadOnlyProvider>
                    </div>
                ) : (
                    <EmptyList
                        button={
                            <IntegrationInstanceConfigurationDialog
                                integrationInstanceConfiguration={
                                    {
                                        environmentId: currentEnvironmentId,
                                    } as IntegrationInstanceConfiguration
                                }
                                triggerNode={<Button label="Create Instance Configuration" />}
                            />
                        }
                        icon={<Settings2Icon className="size-24 text-gray-300" />}
                        message="Get started by creating a new integration instance configuration."
                        title="No Integration Configuration Instances"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default IntegrationInstanceConfigurations;
