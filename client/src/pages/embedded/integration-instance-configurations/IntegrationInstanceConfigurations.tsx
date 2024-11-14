import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import IntegrationInstanceConfigurationWorkflowSheet from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationWorkflowSheet';
import IntegrationInstanceConfigurationsFilterTitle from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationsFilterTitle';
import IntegrationInstanceConfigurationDialog from '@/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import IntegrationInstanceConfigurationList from '@/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Environment, IntegrationInstanceConfiguration} from '@/shared/middleware/embedded/configuration';
import {useGetIntegrationInstanceConfigurationTagsQuery} from '@/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {useGetIntegrationInstanceConfigurationsQuery} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Settings2Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

export enum Type {
    Integration,
    Tag,
    UnifiedAPI,
}

const IntegrationInstanceConfigurations = () => {
    const [searchParams] = useSearchParams();

    const [environment, setEnvironment] = useState<number | undefined>(
        searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined
    );

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
        integrationInstanceConfigurations: true,
    });

    const {
        data: integrationInstanceConfigurations,
        error: integrationInstanceConfigurationsError,
        isLoading: integrationInstanceConfigurationsLoading,
    } = useGetIntegrationInstanceConfigurationsQuery({
        environment:
            environment === undefined ? undefined : environment === 1 ? Environment.Test : Environment.Production,
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
                                            environment:
                                                environment === undefined
                                                    ? undefined
                                                    : environment === 1
                                                      ? Environment.Test
                                                      : Environment.Production,
                                        } as IntegrationInstanceConfiguration
                                    }
                                    triggerNode={<Button>New Instance Configuration</Button>}
                                />
                            )
                        }
                        title={
                            <IntegrationInstanceConfigurationsFilterTitle
                                environment={environment}
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
                                {[
                                    {label: 'All Environments'},
                                    {label: 'Test', value: 1},
                                    {label: 'Production', value: 2},
                                ]?.map((item) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            current: environment === item.value,
                                            id: item.value,
                                            name: item.label,
                                            onItemClick: (id?: number | string) => {
                                                setEnvironment(id as number);
                                            },
                                        }}
                                        key={item.value ?? ''}
                                        toLink={`?environment=${item.value ?? ''}${filterData.id ? `&${filterData.type === Type.Integration ? 'integrationId' : 'tagId'}=${filterData.id}` : ''}`}
                                    />
                                ))}
                            </>
                        }
                        title="Environments"
                    />

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
                                            toLink={`?integrationId=${item.id}&environment=${environment ?? ''}`}
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
                                                toLink={`?tagId=${item.id}&environment=${environment ?? ''}`}
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
                    <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                        {Array.from(integrationInstanceConfigurationMap.keys())?.map(
                            (integrationId) =>
                                integrations &&
                                tags && (
                                    <IntegrationInstanceConfigurationList
                                        componentDefinitions={componentDefinitions}
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

                        <IntegrationInstanceConfigurationWorkflowSheet />
                    </div>
                ) : (
                    <EmptyList
                        button={
                            <IntegrationInstanceConfigurationDialog
                                integrationInstanceConfiguration={
                                    {
                                        environment: environment === 1 ? Environment.Test : Environment.Production,
                                    } as IntegrationInstanceConfiguration
                                }
                                triggerNode={<Button>Create Instance Configuration</Button>}
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
