import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {IntegrationInstanceModel} from '@/middleware/embedded/configuration';
import IntegrationInstanceDialog from '@/pages/embedded/integration-instances/components/IntegrationInstanceDialog';
import {useGetIntegrationInstanceTagsQuery} from '@/queries/embedded/integrationInstanceTags.queries';
import {useGetIntegrationInstancesQuery} from '@/queries/embedded/integrationInstances.queries';
import {useGetIntegrationsQuery} from '@/queries/embedded/integrations.queries';
import {Layers3Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import LayoutContainer from '../../../layouts/LayoutContainer';
import PageHeader from '../../../layouts/PageHeader';
import IntegrationInstanceList from './components/IntegrationInstanceList';

export enum Type {
    Integration,
    Tag,
}

const ConnectedUsers = () => {
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
        data: integrations,
        error: integrationsError,
        isLoading: integrationsIsLoading,
    } = useGetIntegrationsQuery({
        integrationInstances: true,
    });

    const {
        data: integrationInstances,
        error: integrationInstancesError,
        isLoading: integrationInstancesIsLoading,
    } = useGetIntegrationInstancesQuery({
        integrationId: searchParams.get('integrationId') ? parseInt(searchParams.get('integrationId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const integrationInstanceMap: Map<number, IntegrationInstanceModel[]> = new Map<
        number,
        IntegrationInstanceModel[]
    >();

    if (integrationInstances) {
        for (const integrationInstance of integrationInstances) {
            let currentIntegrationInstances: IntegrationInstanceModel[];

            if (integrationInstance.integration) {
                if (integrationInstanceMap.has(integrationInstance.integration.id!)) {
                    currentIntegrationInstances = integrationInstanceMap.get(integrationInstance.integration.id!)!;
                } else {
                    currentIntegrationInstances = [];
                }

                currentIntegrationInstances.push(integrationInstance);

                integrationInstanceMap.set(integrationInstance.integration.id!, currentIntegrationInstances);
            }
        }
    }

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetIntegrationInstanceTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Integration) {
        pageTitle = integrations?.find((integration) => integration.id === filterData.id)?.componentName;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <LayoutContainer
            header={
                <PageHeader
                    centerTitle={true}
                    position="main"
                    right={<IntegrationInstanceDialog triggerNode={<Button>Create Instance</Button>} />}
                    title={`${searchParams.get('tagId') ? 'Tags' : 'Integrations'}: ${pageTitle || 'All'}`}
                />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    bottomBody={
                        <>
                            {!tagsIsLoading &&
                                (tags && !!tags.length ? (
                                    tags?.map((item) => (
                                        <LeftSidebarNavItem
                                            icon={<TagIcon className="mr-1 size-4" />}
                                            item={{
                                                filterData: filterData?.id === item.id && filterData.type === Type.Tag,
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
                                    <span className="px-3 text-xs">You have not created any tags yet.</span>
                                ))}
                        </>
                    }
                    bottomTitle="Tags"
                    topBody={
                        <>
                            <LeftSidebarNavItem
                                item={{
                                    filterData: !filterData?.id && filterData.type === Type.Integration,
                                    name: 'All Integrations',
                                    onItemClick: (id?: number | string) => {
                                        setFilterData({
                                            id: id as number,
                                            type: Type.Integration,
                                        });
                                    },
                                }}
                            />

                            {integrations &&
                                integrations?.map((item) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            filterData:
                                                filterData?.id === item.id && filterData.type === Type.Integration,
                                            id: item.id,
                                            name: item.componentName!,
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
                    topTitle="Integrations"
                />
            }
            leftSidebarHeader={<PageHeader position="sidebar" title="Instances" />}
        >
            <PageLoader
                errors={[integrationsError, integrationInstancesError, tagsError]}
                loading={integrationsIsLoading || integrationInstancesIsLoading || tagsIsLoading}
            >
                {integrationInstances && integrationInstances.length > 0 ? (
                    <div className="w-full px-2 3xl:mx-auto 3xl:w-4/5">
                        {Array.from(integrationInstanceMap.keys())?.map(
                            (integrationId) =>
                                integrations &&
                                tags && (
                                    <IntegrationInstanceList
                                        integration={
                                            integrations.find(
                                                (currentIntegration) => currentIntegration.id === integrationId
                                            )!
                                        }
                                        integrationInstances={integrationInstanceMap.get(integrationId)!}
                                        key={integrationId}
                                        tags={tags}
                                    />
                                )
                        )}
                    </div>
                ) : (
                    <EmptyList
                        button={<IntegrationInstanceDialog triggerNode={<Button>Create Instance</Button>} />}
                        icon={<Layers3Icon className="size-12 text-gray-400" />}
                        message="Get started by creating a new integration instance."
                        title="No instances of integrations"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ConnectedUsers;
