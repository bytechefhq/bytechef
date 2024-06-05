import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import IntegrationInstanceConfigurationDialog from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialog';
import IntegrationInstanceConfigurationList from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationList';
import IntegrationInstanceConfigurationWorkflowSheet from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationWorkflowSheet';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {IntegrationInstanceConfigurationModel} from '@/shared/middleware/embedded/configuration';
import {useGetIntegrationInstanceConfigurationTagsQuery} from '@/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {useGetIntegrationInstanceConfigurationsQuery} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Settings2Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

export enum Type {
    Integration,
    Tag,
}

const IntegrationInstanceConfigurations = () => {
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('integrationId')
            ? parseInt(searchParams.get('integrationId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Integration,
    };

    const [filterData, setFilterData] = useState<{id?: number; type: Type}>(defaultCurrentState);

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
        integrationId: searchParams.get('integrationId') ? parseInt(searchParams.get('integrationId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const integrationInstanceConfigurationMap: Map<number, IntegrationInstanceConfigurationModel[]> = new Map<
        number,
        IntegrationInstanceConfigurationModel[]
    >();

    if (integrationInstanceConfigurations) {
        for (const integrationInstanceConfiguration of integrationInstanceConfigurations) {
            let currentIntegrationInstanceConfigurations: IntegrationInstanceConfigurationModel[];

            if (integrationInstanceConfiguration.integration) {
                if (integrationInstanceConfigurationMap.has(integrationInstanceConfiguration.integration.id!)) {
                    currentIntegrationInstanceConfigurations = integrationInstanceConfigurationMap.get(
                        integrationInstanceConfiguration.integration.id!
                    )!;
                } else {
                    currentIntegrationInstanceConfigurations = [];
                }

                currentIntegrationInstanceConfigurations.push(integrationInstanceConfiguration);

                integrationInstanceConfigurationMap.set(
                    integrationInstanceConfiguration.integration.id!,
                    currentIntegrationInstanceConfigurations
                );
            }
        }
    }

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetIntegrationInstanceConfigurationTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Integration) {
        pageTitle = integrations?.find((integration) => integration.id === filterData.id)?.componentName;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

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
                                    triggerNode={<Button>New Instance Configuration</Button>}
                                />
                            )
                        }
                        title={
                            !pageTitle
                                ? 'All Instance Configurations'
                                : `Filter by ${searchParams.get('tagId') ? 'tag' : 'integration'}: ${pageTitle}`
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
                                        onItemClick: (id?: number | string) => {
                                            setFilterData({
                                                id: id as number,
                                                type: Type.Integration,
                                            });
                                        },
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
                                                onItemClick: (id?: number | string) => {
                                                    setFilterData({
                                                        id: id as number,
                                                        type: Type.Integration,
                                                    });
                                                },
                                            }}
                                            key={item.componentName}
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
                                                    onItemClick: (id?: number | string) => {
                                                        setFilterData({
                                                            id: id as number,
                                                            type: Type.Tag,
                                                        });
                                                    },
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
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Instance Configurations" />}
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
                                triggerNode={<Button>Create Instance Configuration</Button>}
                            />
                        }
                        icon={<Settings2Icon className="size-12 text-gray-400" />}
                        message="Get started by creating a new integration instance configuration."
                        title="No Integration Configuration Instances"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default IntegrationInstanceConfigurations;
