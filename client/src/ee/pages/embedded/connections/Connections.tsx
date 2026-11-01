import Button from '@/components/Button/Button';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import ConnectionsFilterTitle from '@/ee/pages/embedded/connections/components/ConnectionsFilterTitle';
import {EMBEDDED_CONNECTIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
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
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
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

    const environmentScopeNote = useEnvironmentScopeNote('Connections');

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

    const isFirstRun = !hasActiveFilter && connections?.length === 0;

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        connections &&
                        connections.length > 0 &&
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
                                showSharedOption
                                triggerNode={<Button label="New Connection" />}
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                            />
                        )
                    }
                    title={
                        ((connections && connections.length > 0) || hasActiveFilter) && componentDefinitions ? (
                            <ConnectionsFilterTitle
                                componentDefinitions={componentDefinitions}
                                filterData={filterData}
                                tags={tags}
                            />
                        ) : (
                            isFirstRun && 'Connections'
                        )
                    }
                />
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
            leftSidebarHidden={isFirstRun}
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
                ) : hasActiveFilter ? (
                    <EmptyFilterResult entityName="connections" entityTitle="Connections" />
                ) : (
                    <EmptyState
                        actions={
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
                                    showSharedOption
                                    triggerNode={<Button label="Create Connection" />}
                                    useCreateConnectionMutation={useCreateConnectionMutation}
                                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                                />
                            )
                        }
                        description="A connection stores the credentials an app needs, like an API key or an OAuth sign-in, so integration workflows can act in that app. Mark a connection as shared and every connected user in the environment can use it."
                        docsLink={{href: EMBEDDED_CONNECTIONS_DOCUMENTATION_URL, label: 'How connections work'}}
                        icon={<Link2Icon />}
                        note={environmentScopeNote}
                        title="Create your first connection"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
