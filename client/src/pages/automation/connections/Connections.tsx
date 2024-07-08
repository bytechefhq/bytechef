import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ConnectionEnvironmentModel} from '@/shared/middleware/automation/connection';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Link2Icon, TagIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import ConnectionList from './components/ConnectionList';

export enum Type {
    Component,
    Tag,
}

export const Connections = () => {
    const [searchParams] = useSearchParams();

    const [environment, setEnvironment] = useState<number | undefined>(getEnvironment());
    const [filterData, setFilterData] = useState<{
        id?: number | string;
        type: Type;
    }>(getFilterData());

    const {currentWorkspaceId} = useWorkspaceStore();

    const {
        data: allConnections,
        error: allConnectionsError,
        isLoading: allConnectionsIsLoading,
    } = useGetWorkspaceConnectionsQuery({id: currentWorkspaceId!});

    const allComponentNames = allConnections?.map((connection) => connection.componentName);

    const {data: componentDefinitions, isLoading: componentsLoading} = useGetComponentDefinitionsQuery(
        {include: allComponentNames},
        allComponentNames !== undefined
    );

    const {
        data: connections,
        error: connectionsError,
        isLoading: connectionsIsLoading,
    } = useGetWorkspaceConnectionsQuery({
        componentName: searchParams.get('componentName') ? searchParams.get('componentName')! : undefined,
        environment:
            environment === 1
                ? ConnectionEnvironmentModel.Development
                : environment === 2
                  ? ConnectionEnvironmentModel.Test
                  : environment === 3
                    ? ConnectionEnvironmentModel.Production
                    : undefined,
        id: currentWorkspaceId!,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Component) {
        pageTitle = componentDefinitions?.find(
            (componentDefinition) => componentDefinition.name === filterData.id
        )?.title;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    function getEnvironment() {
        return searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined;
    }

    function getFilterData() {
        return searchParams.get('componentName') || searchParams.get('tagId')
            ? {
                  id: searchParams.get('componentName')
                      ? searchParams.get('componentName')!
                      : parseInt(searchParams.get('tagId')!),
                  type: searchParams.get('tagId') ? Type.Tag : Type.Component,
              }
            : {type: Type.Component};
    }

    useEffect(() => {
        setEnvironment(getEnvironment());
        setFilterData(getFilterData());

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchParams]);

    return (
        <LayoutContainer
            header={
                connections &&
                connections.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <ConnectionDialog
                                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                connectionsQueryKey={ConnectionKeys.connections}
                                triggerNode={<Button>New Connection</Button>}
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                            />
                        }
                        title={
                            !pageTitle
                                ? 'All Connections'
                                : `Filter by ${searchParams.get('tagId') ? 'tag' : 'component'}: ${pageTitle}`
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
                                    {label: 'All Environments', value: undefined},
                                    {label: 'Development', value: 1},
                                    {label: 'Test', value: 2},
                                    {label: 'Production', value: 3},
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
                                        toLink={`?environment=${item.value ?? ''}${filterData.id ? `&${filterData.type === Type.Component ? 'componentName' : 'tagId'}=${filterData.id}` : ''}`}
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
                                        current: !filterData?.id && filterData.type === Type.Component,
                                        name: 'All Components',
                                        onItemClick: (id?: number | string) => {
                                            setFilterData({
                                                id,
                                                type: Type.Component,
                                            });
                                        },
                                    }}
                                    toLink={`?environment=${environment}`}
                                />

                                {!componentsLoading &&
                                    componentDefinitions?.map((item) => (
                                        <LeftSidebarNavItem
                                            item={{
                                                current:
                                                    filterData?.id === item.name && filterData.type === Type.Component,
                                                id: item.name!,
                                                name: item.title!,
                                                onItemClick: (id?: number | string) =>
                                                    setFilterData({
                                                        id,
                                                        type: Type.Component,
                                                    }),
                                            }}
                                            key={item.name}
                                            toLink={`?componentName=${item.name}&environment=${environment ?? ''}`}
                                        />
                                    ))}
                            </>
                        }
                        title="Components"
                    />

                    <LeftSidebarNav
                        body={
                            <>
                                {!tagsIsLoading &&
                                    (!tags?.length ? (
                                        <p className="px-3 text-xs">No tags.</p>
                                    ) : (
                                        tags?.map((item) => (
                                            <LeftSidebarNavItem
                                                icon={<TagIcon className="mr-1 size-4" />}
                                                item={{
                                                    current: filterData?.id === item.id && filterData.type === Type.Tag,
                                                    id: item.id!,
                                                    name: item.name,
                                                    onItemClick: (id?: number | string) => {
                                                        setFilterData({
                                                            id,
                                                            type: Type.Tag,
                                                        });
                                                    },
                                                }}
                                                key={item.id}
                                                toLink={`?tagId=${item.id}&environment=${environment ?? ''}`}
                                            />
                                        ))
                                    ))}
                            </>
                        }
                        title="Tags"
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Connections" />}
        >
            <PageLoader
                errors={[allConnectionsError, connectionsError, tagsError]}
                loading={allConnectionsIsLoading || connectionsIsLoading || tagsIsLoading}
            >
                {connections && connections?.length > 0 ? (
                    connections && tags && <ConnectionList connections={connections} tags={tags} />
                ) : (
                    <EmptyList
                        button={
                            <ConnectionDialog
                                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                connectionsQueryKey={ConnectionKeys.connections}
                                triggerNode={<Button>Create Connection</Button>}
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                            />
                        }
                        icon={<Link2Icon className="size-24 text-gray-300" />}
                        message="You do not have any Connections created yet."
                        title="No Connections"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
