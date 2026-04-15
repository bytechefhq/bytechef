import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import ConnectionsFilterTitle from '@/pages/automation/connections/components/ConnectionsFilterTitle';
import {useVisibilityFeatureEnabled} from '@/pages/automation/connections/hooks/useVisibilityFeatureEnabled';
import {buildBulkPromoteToast} from '@/pages/automation/connections/utils/bulkPromoteToast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Connection} from '@/shared/middleware/automation/configuration';
import {usePromoteAllPrivateConnectionsToWorkspaceMutation} from '@/shared/middleware/graphql';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {Link2Icon, TagIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

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

    const {enabled: visibilityFeatureEnabled, isAdmin} = useVisibilityFeatureEnabled();

    const queryClient = useQueryClient();

    const [showBulkPromoteConfirm, setShowBulkPromoteConfirm] = useState(false);

    const promoteAllPrivateMutation = usePromoteAllPrivateConnectionsToWorkspaceMutation({
        onError: () => {
            setShowBulkPromoteConfirm(false);
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});

            const {message, type} = buildBulkPromoteToast(data.promoteAllPrivateConnectionsToWorkspace);

            if (type === 'error') {
                toast.error(message);
            } else {
                toast(message);
            }

            setShowBulkPromoteConfirm(false);
        },
    });

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

    const allComponentNames = useMemo(
        () =>
            Array.from(
                new Set(
                    (hasActiveFilter ? unfilteredConnections : connections)?.map(
                        (connection) => connection.componentName
                    )
                )
            ),
        [connections, hasActiveFilter, unfilteredConnections]
    );

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

    const isAnyLoading = componentsLoading || connectionsIsLoading || tagsIsLoading || unfilteredConnectionsIsLoading;

    const privateConnectionCount = useMemo(() => {
        const source = hasActiveFilter ? unfilteredConnections : connections;

        return (source ?? []).filter((connection) => (connection.visibility || 'PRIVATE') === 'PRIVATE').length;
    }, [connections, hasActiveFilter, unfilteredConnections]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        connections && connections.length > 0 && componentDefinitions ? (
                            <div className="flex items-center gap-4">
                                <EnvironmentSelect />

                                {visibilityFeatureEnabled &&
                                    isAdmin &&
                                    currentWorkspaceId &&
                                    privateConnectionCount > 0 && (
                                        <Button
                                            disabled={promoteAllPrivateMutation.isPending}
                                            label={`Promote ${privateConnectionCount} private to workspace`}
                                            onClick={() => setShowBulkPromoteConfirm(true)}
                                            title="Promote every PRIVATE connection in this workspace to WORKSPACE visibility (CE→EE migration helper)"
                                            variant="outline"
                                        />
                                    )}

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
                            </div>
                        ) : (
                            !isAnyLoading && <EnvironmentSelect />
                        )
                    }
                    title={
                        connections && connections.length > 0 && componentDefinitions ? (
                            <ConnectionsFilterTitle
                                componentDefinitions={componentDefinitions}
                                filterData={filterData}
                                tags={tags}
                            />
                        ) : (
                            ''
                        )
                    }
                />
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

            <AlertDialog
                onOpenChange={(isOpen) => !isOpen && setShowBulkPromoteConfirm(false)}
                open={showBulkPromoteConfirm}
            >
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Promote {privateConnectionCount} private connection(s)?</AlertDialogTitle>

                        <AlertDialogDescription>
                            Every PRIVATE connection in this workspace will become visible to all workspace members.
                            This cannot be undone in bulk — you can demote individual connections back to private
                            afterwards.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowBulkPromoteConfirm(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            disabled={promoteAllPrivateMutation.isPending}
                            onClick={() => promoteAllPrivateMutation.mutate({workspaceId: String(currentWorkspaceId)})}
                        >
                            Promote all
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </LayoutContainer>
    );
};
