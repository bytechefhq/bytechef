import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ConnectionsFilterTitle from '@/pages/automation/connections/components/ConnectionsFilterTitle';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Connection} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Link2Icon, TagIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

import ConnectionList from './components/connection-list/ConnectionList';

export enum Type {
    Component,
    Tag,
}

export const Connections = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();

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
    } = useGetWorkspaceConnectionsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
    });

    const allComponentNames = Array.from(new Set(allConnections?.map((connection) => connection.componentName)));

    const {data: componentDefinitions, isLoading: componentsLoading} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {
        data: connections,
        error: connectionsError,
        isLoading: connectionsIsLoading,
    } = useGetWorkspaceConnectionsQuery({
        componentName: searchParams.get('componentName') ? searchParams.get('componentName')! : undefined,
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

    return (
        <LayoutContainer
            header={
                connections &&
                connections.length > 0 &&
                componentDefinitions && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <ConnectionDialog
                                componentDefinitions={componentDefinitions}
                                connection={
                                    {
                                        environmentId: currentEnvironmentId,
                                    } as Connection
                                }
                                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                connectionsQueryKey={ConnectionKeys.connections}
                                triggerNode={<Button label="New Connection" />}
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                            />
                        }
                        title={
                            <ConnectionsFilterTitle
                                componentDefinitions={componentDefinitions}
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
                                <LeftSidebarNavItem
                                    item={{
                                        current: !filterData?.id && filterData.type === Type.Component,
                                        name: 'All Components',
                                    }}
                                    toLink=""
                                />

                                {!componentsLoading &&
                                    componentDefinitions
                                        ?.filter((componentDefinition) =>
                                            allComponentNames.includes(componentDefinition.name)
                                        )
                                        ?.map((item) => (
                                            <LeftSidebarNavItem
                                                item={{
                                                    current:
                                                        filterData?.id === item.name &&
                                                        filterData.type === Type.Component,
                                                    id: item.name!,
                                                    name: item.title!,
                                                }}
                                                key={item.name}
                                                toLink={`?componentName=${item.name}`}
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
                                                toLink={`?tagId=${item.id}`}
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
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[allConnectionsError, connectionsError, tagsError]}
                loading={allConnectionsIsLoading || componentsLoading || connectionsIsLoading || tagsIsLoading}
            >
                {componentDefinitions && connections && connections?.length > 0 ? (
                    connections &&
                    tags && (
                        <ConnectionList
                            componentDefinitions={componentDefinitions}
                            connections={connections}
                            tags={tags}
                        />
                    )
                ) : (
                    <EmptyList
                        button={
                            componentDefinitions && (
                                <ConnectionDialog
                                    componentDefinitions={componentDefinitions}
                                    connection={
                                        {
                                            environmentId: currentEnvironmentId,
                                        } as Connection
                                    }
                                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                    connectionsQueryKey={ConnectionKeys.connections}
                                    triggerNode={<Button label="Create Connection" />}
                                    useCreateConnectionMutation={useCreateConnectionMutation}
                                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                                />
                            )
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
