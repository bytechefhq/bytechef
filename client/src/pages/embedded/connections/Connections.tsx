import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ConnectionsFilterTitle from '@/pages/embedded/connections/components/ConnectionsFilterTitle';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Connection, ConnectionEnvironment} from '@/shared/middleware/embedded/connection';
import {useCreateConnectionMutation} from '@/shared/mutations/embedded/connections.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/shared/queries/embedded/connections.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Link2Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import ConnectionList from './components/connection-list/ConnectionList';

export enum Type {
    Component,
    Tag,
}

export const Connections = () => {
    const [searchParams] = useSearchParams();

    const [environment, setEnvironment] = useState<number | undefined>(
        searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined
    );

    const componentName = searchParams.get('componentName');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: componentName ? componentName : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Component,
    };

    const {
        data: allConnections,
        error: allConnectionsError,
        isLoading: allConnectionsIsLoading,
    } = useGetConnectionsQuery({});

    const allComponentNames = allConnections?.map((connection) => connection.componentName);

    const {data: componentDefinitions, isLoading: componentsLoading} = useGetComponentDefinitionsQuery(
        {include: allComponentNames ? allComponentNames : ['e']},
        allComponentNames !== undefined
    );

    const {
        data: connections,
        error: connectionsError,
        isLoading: connectionsIsLoading,
    } = useGetConnectionsQuery({
        componentName: searchParams.get('componentName') ? searchParams.get('componentName')! : undefined,
        environment:
            environment === undefined
                ? undefined
                : environment === 1
                  ? ConnectionEnvironment.Development
                  : environment === 2
                    ? ConnectionEnvironment.Test
                    : ConnectionEnvironment.Production,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

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
                                connection={
                                    {
                                        environment:
                                            environment === undefined
                                                ? undefined
                                                : environment === 1
                                                  ? ConnectionEnvironment.Development
                                                  : environment === 2
                                                    ? ConnectionEnvironment.Test
                                                    : ConnectionEnvironment.Production,
                                    } as Connection
                                }
                                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                connectionsQueryKey={ConnectionKeys.connections}
                                triggerNode={<Button>New Connection</Button>}
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                            />
                        }
                        title={
                            <ConnectionsFilterTitle
                                componentDefinitions={componentDefinitions}
                                environment={environment}
                                filterData={filterData}
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
                                    }}
                                />

                                {!componentsLoading &&
                                    componentDefinitions?.map((item) => (
                                        <LeftSidebarNavItem
                                            item={{
                                                current:
                                                    filterData?.id === item.name && filterData.type === Type.Component,
                                                id: item.name!,
                                                name: item.title!,
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
            leftSidebarWidth="72"
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
                                connection={
                                    {
                                        environment:
                                            environment === 1
                                                ? ConnectionEnvironment.Development
                                                : environment === 2
                                                  ? ConnectionEnvironment.Test
                                                  : ConnectionEnvironment.Production,
                                    } as Connection
                                }
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
