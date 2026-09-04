import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ConnectionsFilterTitle from '@/ee/pages/embedded/connections/components/ConnectionsFilterTitle';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {useCreateConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/ee/shared/queries/embedded/connections.queries';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionsLeftSidebarNav from '@/shared/components/connection/ConnectionsLeftSidebarNav';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Link2Icon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

import ConnectionList from './components/connection-list/ConnectionList';

export enum Type {
    Component,
    Tag,
}

export const Connections = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    const componentName = searchParams.get('componentName');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: componentName ? componentName : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Component,
    };

    const hasActiveFilter = !!componentName || !!tagId;

    const {data: componentDefinitions, isLoading: componentsLoading} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {
        data: connections,
        error: connectionsError,
        isLoading: connectionsIsLoading,
    } = useGetConnectionsQuery({
        componentName: componentName ? componentName : undefined,
        environmentId: currentEnvironmentId,
        tagId: tagId ? parseInt(tagId) : undefined,
    });

    const {
        data: unfilteredConnections,
        error: unfilteredConnectionsError,
        isLoading: unfilteredConnectionsIsLoading,
    } = useGetConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
        },
        hasActiveFilter
    );

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

    const componentRowsAreLoading = componentsLoading || connectionsIsLoading || unfilteredConnectionsIsLoading;

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
                <ConnectionsLeftSidebarNav
                    componentDefinitions={componentDefinitions}
                    connections={hasActiveFilter ? unfilteredConnections : connections}
                    connectionsAreLoading={componentRowsAreLoading}
                    currentComponentName={componentName ?? undefined}
                    currentTagId={tagId ? parseInt(tagId) : undefined}
                    tags={tags}
                    tagsIsLoading={tagsIsLoading}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Connections" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[connectionsError, tagsError, unfilteredConnectionsError]}
                loading={componentsLoading || connectionsIsLoading || tagsIsLoading || unfilteredConnectionsIsLoading}
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
