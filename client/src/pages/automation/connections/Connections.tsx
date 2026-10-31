import Button from '@/components/Button/Button';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import ConnectionsFilterTitle from '@/pages/automation/connections/components/ConnectionsFilterTitle';
import {CONNECTIONS_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionsLeftSidebarNav from '@/shared/components/connection/ConnectionsLeftSidebarNav';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Connection} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();

    const {captureEmptyStateAction} = useAnalytics();

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
    } = useGetWorkspaceConnectionsQuery({
        componentName: componentName ? componentName : undefined,
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
        tagId: tagId ? parseInt(tagId) : undefined,
    });

    const {
        data: unfilteredConnections,
        error: unfilteredConnectionsError,
        isLoading: unfilteredConnectionsIsLoading,
    } = useGetWorkspaceConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
        },
        hasActiveFilter
    );

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

    const isAnyLoading = componentsLoading || connectionsIsLoading || tagsIsLoading || unfilteredConnectionsIsLoading;

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
                                platformType={PlatformType.AUTOMATION}
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
            <PageLoader errors={[connectionsError, tagsError, unfilteredConnectionsError]} loading={isAnyLoading}>
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
                                    platformType={PlatformType.AUTOMATION}
                                    triggerNode={
                                        <Button
                                            label="Create Connection"
                                            onClick={() => captureEmptyStateAction('connections', 'create_connection')}
                                        />
                                    }
                                    useCreateConnectionMutation={useCreateConnectionMutation}
                                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                                />
                            )
                        }
                        description="A connection stores the credentials an app needs, like an API key or an OAuth sign-in, so workflow steps can act for you. You can also create one from a step while building a workflow."
                        docsLink={{href: CONNECTIONS_DOCUMENTATION_URL, label: 'How connections work'}}
                        icon={<Link2Icon />}
                        note={environmentScopeNote}
                        title="Connect your first app"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
