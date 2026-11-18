import Button from '@/components/Button/Button';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import ConnectedUserFilterNav, {
    getConnectedUserLabel,
} from '@/ee/pages/embedded/connections/components/ConnectedUserFilterNav';
import ConnectionsFilterTitle from '@/ee/pages/embedded/connections/components/ConnectionsFilterTitle';
import {EMBEDDED_CONNECTIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import {Connection, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {useCreateConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetConnectedUserQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {
    ConnectionKeys,
    useGetAllConnectedUserConnectionsQuery,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/ee/shared/queries/embedded/connections.queries';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionsLeftSidebarNav from '@/shared/components/connection/ConnectionsLeftSidebarNav';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Link2Icon} from 'lucide-react';
import {useEffect, useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

import ConnectionList from './components/connection-list/ConnectionList';

export enum Type {
    Component,
    Tag,
}

const CONNECTION_STEPS = [
    {id: 'createConnection', label: 'Create a connection', status: 'current' as const},
    {id: 'shareConnection', label: 'Share it with connected users', status: 'upcoming' as const},
    {id: 'useInWorkflow', label: 'Use it in a workflow', status: 'upcoming' as const},
];

export const Connections = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams, setSearchParams] = useSearchParams();

    const environmentScopeNote = useEnvironmentScopeNote('Connections');

    const componentName = searchParams.get('componentName');
    const tagId = searchParams.get('tagId');
    const connectedUserIdParam = searchParams.get('connectedUserId');

    const parsedConnectedUserId = connectedUserIdParam !== null ? Number(connectedUserIdParam) : undefined;

    const connectedUserIdInvalid =
        parsedConnectedUserId !== undefined && !(Number.isInteger(parsedConnectedUserId) && parsedConnectedUserId > 0);

    const connectedUserId = connectedUserIdInvalid ? undefined : parsedConnectedUserId;

    const connectedUserView = connectedUserId !== undefined;

    const filterData = {
        id: componentName ? componentName : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Component,
    };

    const hasComponentOrTagFilter = !!componentName || !!tagId;

    const hasActiveFilter = hasComponentOrTagFilter || connectedUserView;

    const {data: componentDefinitions, isLoading: componentsLoading} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {
        data: connections,
        error: connectionsError,
        isLoading: connectionsIsLoading,
    } = useGetConnectionsQuery(
        {
            componentName: componentName ? componentName : undefined,
            environmentId: currentEnvironmentId,
            tagId: tagId ? parseInt(tagId) : undefined,
        },
        !connectedUserView
    );

    const initialLoadComplete = useInitialLoadComplete(connectionsIsLoading);

    const {
        data: unfilteredConnections,
        error: unfilteredConnectionsError,
        isLoading: unfilteredConnectionsIsLoading,
    } = useGetConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
        },
        hasComponentOrTagFilter && !connectedUserView
    );

    const {
        data: connectedUser,
        error: connectedUserError,
        isLoading: connectedUserIsLoading,
    } = useGetConnectedUserQuery(connectedUserId ?? 0, connectedUserView);

    const {
        data: connectedUserConnections,
        error: connectedUserConnectionsError,
        isLoading: connectedUserConnectionsIsLoading,
    } = useGetAllConnectedUserConnectionsQuery(connectedUserId ?? 0, connectedUserView);

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

    const filteredConnectedUserConnections = useMemo(
        () =>
            connectedUserConnections?.filter(
                (connection) =>
                    (!componentName || connection.componentName === componentName) &&
                    (!tagId || !!connection.tags?.some((tag) => tag.id === parseInt(tagId)))
            ),
        [componentName, connectedUserConnections, tagId]
    );

    const connectedUserTags = useMemo(() => {
        const tagsById = new Map<number, Tag>();

        connectedUserConnections?.forEach((connection) =>
            connection.tags?.forEach((tag) => {
                if (tag.id != null) {
                    tagsById.set(tag.id, tag);
                }
            })
        );

        return [...tagsById.values()];
    }, [connectedUserConnections]);

    const connectedUserInOtherEnvironment =
        connectedUser !== undefined && connectedUser.environmentId !== currentEnvironmentId;

    const dropConnectedUserParam = connectedUserIdInvalid || (connectedUserView && connectedUserInOtherEnvironment);

    const connectedUserReady = connectedUser !== undefined && !connectedUserInOtherEnvironment;

    const visibleConnections = connectedUserView
        ? connectedUserReady
            ? filteredConnectedUserConnections
            : []
        : connections;

    const sidebarConnections = connectedUserView
        ? connectedUserReady
            ? connectedUserConnections
            : []
        : hasComponentOrTagFilter
          ? unfilteredConnections
          : connections;

    const componentRowsAreLoading =
        componentsLoading ||
        connectedUserIsLoading ||
        connectionsIsLoading ||
        unfilteredConnectionsIsLoading ||
        connectedUserConnectionsIsLoading;

    const isFirstRun = !hasActiveFilter && connections?.length === 0;

    const handleConnectedUserSelect = (selectedConnectedUserId?: number) =>
        setSearchParams(
            selectedConnectedUserId !== undefined ? {connectedUserId: String(selectedConnectedUserId)} : {}
        );

    useEffect(() => {
        if (!dropConnectedUserParam) {
            return;
        }

        const nextSearchParams = new URLSearchParams(searchParams);

        nextSearchParams.delete('connectedUserId');

        setSearchParams(nextSearchParams, {replace: true});
    }, [dropConnectedUserParam, searchParams, setSearchParams]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        !connectedUserView &&
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
                        ((visibleConnections && visibleConnections.length > 0) || hasActiveFilter) &&
                        componentDefinitions ? (
                            <ConnectionsFilterTitle
                                componentDefinitions={componentDefinitions}
                                connectedUserName={
                                    connectedUser && connectedUserReady
                                        ? getConnectedUserLabel(connectedUser)
                                        : undefined
                                }
                                filterData={filterData}
                                tags={connectedUserView ? connectedUserTags : tags}
                            />
                        ) : (
                            isFirstRun && 'Connections'
                        )
                    }
                />
            }
            leftSidebarBody={
                <>
                    <ConnectedUserFilterNav
                        environmentId={currentEnvironmentId}
                        onSelect={handleConnectedUserSelect}
                        selectedConnectedUserId={connectedUserId}
                    />

                    <ConnectionsLeftSidebarNav
                        componentDefinitions={componentDefinitions}
                        connections={sidebarConnections}
                        connectionsAreLoading={componentRowsAreLoading}
                        currentComponentName={componentName ?? undefined}
                        currentTagId={tagId ? parseInt(tagId) : undefined}
                        preservedSearchParams={
                            connectedUserView ? {connectedUserId: String(connectedUserId)} : undefined
                        }
                        tags={connectedUserView ? connectedUserTags : tags}
                        tagsIsLoading={tagsIsLoading}
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Connections" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[
                    connectionsError,
                    tagsError,
                    unfilteredConnectionsError,
                    connectedUserError,
                    connectedUserConnectionsError,
                ]}
                loading={componentRowsAreLoading || tagsIsLoading}
            >
                {componentDefinitions && visibleConnections && visibleConnections.length > 0 ? (
                    tags && (
                        <ConnectionList
                            componentDefinitions={componentDefinitions}
                            connectedUserId={connectedUserId}
                            connectedUserIntegrationInstances={connectedUser?.integrationInstances}
                            connections={visibleConnections}
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
                    >
                        <JourneySteps aria-label="Getting started" steps={CONNECTION_STEPS} />
                    </EmptyState>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
