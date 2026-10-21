import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {Breadcrumb, BreadcrumbItem, BreadcrumbList} from '@/components/ui/breadcrumb';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import AppActivityTab from '@/pages/automation/apps/components/AppActivityTab';
import AppApiCollectionsTab from '@/pages/automation/apps/components/AppApiCollectionsTab';
import AppBindingsTab from '@/pages/automation/apps/components/AppBindingsTab';
import AppDeleteDialog from '@/pages/automation/apps/components/AppDeleteDialog';
import AppDeploymentSummary from '@/pages/automation/apps/components/AppDeploymentSummary';
import AppDeploymentsTab from '@/pages/automation/apps/components/AppDeploymentsTab';
import AppDialog from '@/pages/automation/apps/components/AppDialog';
import AppInstanceSelect from '@/pages/automation/apps/components/AppInstanceSelect';
import AppInstancesTab from '@/pages/automation/apps/components/AppInstancesTab';
import AppLeftSidebar from '@/pages/automation/apps/components/AppLeftSidebar';
import AppOverviewTab from '@/pages/automation/apps/components/AppOverviewTab';
import AppStatusSwitch from '@/pages/automation/apps/components/AppStatusSwitch';
import AppWorkflowsTab from '@/pages/automation/apps/components/AppWorkflowsTab';
import {APP_MANAGE_SCOPE} from '@/pages/automation/apps/constants';
import useAppDeployScopes from '@/pages/automation/apps/hooks/useAppDeployScopes';
import useAppManageScopes from '@/pages/automation/apps/hooks/useAppManageScopes';
import useAppOwnerNames from '@/pages/automation/apps/hooks/useAppOwnerNames';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    App as AppType,
    AppInstance,
    AppInstanceStatus,
    useAppQuery,
    useMyWorkspaceScopesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    ActivityIcon,
    BoxesIcon,
    EditIcon,
    EllipsisVerticalIcon,
    LinkIcon,
    NetworkIcon,
    PlusIcon,
    RocketIcon,
    TrashIcon,
    WorkflowIcon,
} from 'lucide-react';
import {ReactNode, useCallback, useMemo, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const INSTANCE_TAB_VALUES = ['activity', 'api-collections', 'connections', 'deployments', 'workflows'];

const AppDetail = () => {
    const [activeTab, setActiveTab] = useState('overview');
    const [selectedAppInstanceId, setSelectedAppInstanceId] = useState<string | undefined>();
    const [showCreateDialog, setShowCreateDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {id} = useParams<{id: string}>();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const getOwnerName = useAppOwnerNames();

    const navigate = useNavigate();

    const appId = id ?? '';

    const {data, error, isLoading} = useAppQuery({id: appId}, {enabled: !!appId});

    const {data: scopesData} = useMyWorkspaceScopesQuery({workspaceId: String(currentWorkspaceId)});

    const canManage = (scopesData?.myWorkspaceScopes ?? []).includes(APP_MANAGE_SCOPE);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const fetchedApp = data?.app as AppType | undefined;

    const app = useMemo(
        () =>
            fetchedApp && {
                ...fetchedApp,
                foreignInstances: fetchedApp.foreignInstances.filter(
                    (foreignAppInstance) => Number(foreignAppInstance.environmentId) === currentEnvironmentId
                ),
                instances: fetchedApp.instances.filter(
                    (appInstance) => Number(appInstance.environmentId) === currentEnvironmentId
                ),
            },
        [currentEnvironmentId, fetchedApp]
    );

    const appInstances = useMemo(() => app?.instances ?? [], [app?.instances]);

    const manageTargets = useMemo(
        () =>
            appInstances.map((appInstance) => ({
                environmentId: appInstance.environmentId,
                workspaceId: appInstance.workspaceId,
            })),
        [appInstances]
    );

    const canManageWorkspace = useAppManageScopes(manageTargets);
    const canDeployWorkspace = useAppDeployScopes(manageTargets);

    const canManageInstance = useCallback(
        (appInstance: AppInstance) =>
            appInstance.status !== AppInstanceStatus.Terminated &&
            canManageWorkspace(appInstance.workspaceId, appInstance.environmentId),
        [canManageWorkspace]
    );

    const canDeployInstance = useCallback(
        (appInstance: AppInstance) =>
            appInstance.status !== AppInstanceStatus.Terminated &&
            canDeployWorkspace(appInstance.workspaceId, appInstance.environmentId),
        [canDeployWorkspace]
    );

    const selectedAppInstance =
        appInstances.find((appInstance) => appInstance.id === selectedAppInstanceId) ?? appInstances[0];

    const renderInstanceTab = (renderTab: (appInstance: AppInstance) => ReactNode) =>
        selectedAppInstance ? (
            renderTab(selectedAppInstance)
        ) : (
            <EmptyState
                description="Create an instance on the Instances tab, or ask its owner to share one with you."
                icon={<BoxesIcon />}
                title="No instance to show"
            />
        );

    return (
        <LayoutContainer
            header={
                <Header
                    position="main"
                    right={
                        app &&
                        canManage && (
                            <div className="flex items-center gap-2">
                                <AppStatusSwitch app={app} canManage={canManage} />

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button
                                            aria-label="App actions"
                                            icon={<EllipsisVerticalIcon />}
                                            size="icon"
                                            variant="ghost"
                                        />
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end" className="p-0">
                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={() => setShowEditDialog(true)}
                                        >
                                            <EditIcon /> Edit
                                        </DropdownMenuItem>

                                        <DropdownMenuSeparator className="m-0" />

                                        <DropdownMenuItem
                                            className="dropdown-menu-item-destructive"
                                            onClick={() => setShowDeleteDialog(true)}
                                            variant="destructive"
                                        >
                                            <TrashIcon /> Delete
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>

                                {showEditDialog && (
                                    <AppDialog
                                        app={app}
                                        canShare={canManage}
                                        onOpenChange={(nextOpen) => {
                                            if (!nextOpen) {
                                                setShowEditDialog(false);
                                            }
                                        }}
                                        open={showEditDialog}
                                    />
                                )}
                            </div>
                        )
                    }
                    title={
                        <Breadcrumb className="min-w-0">
                            <BreadcrumbList className="flex-nowrap">
                                <ListPageBreadcrumbItem label="Apps" to="/automation/apps" />

                                <BreadcrumbItem className="min-w-0 text-content-neutral-primary">
                                    <h1 className="truncate font-semibold">{app?.name || 'Loading...'}</h1>
                                </BreadcrumbItem>
                            </BreadcrumbList>
                        </Breadcrumb>
                    }
                />
            }
            leftSidebarBody={<AppLeftSidebar currentId={appId} />}
            leftSidebarHeader={
                <Header
                    position="sidebar"
                    right={
                        canManage && (
                            <Button
                                aria-label="New App"
                                icon={<PlusIcon className="size-4" />}
                                onClick={() => setShowCreateDialog(true)}
                                size="icon"
                                variant="ghost"
                            />
                        )
                    }
                    title="Apps"
                />
            }
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {app && (
                    <div className="flex h-full w-full flex-col p-4 pt-1">
                        <Tabs className="flex-1" onValueChange={setActiveTab} value={activeTab}>
                            <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                                <TabsList className="max-w-full justify-start overflow-x-auto">
                                    <TabsTrigger value="overview">Overview</TabsTrigger>

                                    <TabsTrigger value="instances">
                                        <BoxesIcon className="mr-2 size-4 max-sm:hidden" />
                                        Instances
                                    </TabsTrigger>

                                    <TabsTrigger value="connections">
                                        <LinkIcon className="mr-2 size-4 max-sm:hidden" />
                                        Connections
                                    </TabsTrigger>

                                    <TabsTrigger value="api-collections">
                                        <NetworkIcon className="mr-2 size-4 max-sm:hidden" />
                                        API Collections
                                    </TabsTrigger>

                                    <TabsTrigger value="workflows">
                                        <WorkflowIcon className="mr-2 size-4 max-sm:hidden" />
                                        Workflows
                                    </TabsTrigger>

                                    <TabsTrigger value="deployments">
                                        <RocketIcon className="mr-2 size-4 max-sm:hidden" />
                                        Deployments
                                    </TabsTrigger>

                                    <TabsTrigger value="activity">
                                        <ActivityIcon className="mr-2 size-4 max-sm:hidden" />
                                        Activity
                                    </TabsTrigger>
                                </TabsList>

                                {INSTANCE_TAB_VALUES.includes(activeTab) && appInstances.length > 1 && (
                                    <AppInstanceSelect
                                        appInstances={appInstances}
                                        onValueChange={setSelectedAppInstanceId}
                                        value={selectedAppInstance?.id}
                                    />
                                )}
                            </div>

                            {activeTab === 'deployments' && selectedAppInstance && (
                                <div className="mb-4 empty:hidden">
                                    <AppDeploymentSummary
                                        appInstanceId={selectedAppInstance.id}
                                        key={selectedAppInstance.id}
                                    />
                                </div>
                            )}

                            <TabsContent className="flex flex-1 flex-col" value="overview">
                                <AppOverviewTab app={app} ownerName={getOwnerName(app.ownerUserId)} />
                            </TabsContent>

                            <TabsContent className="flex flex-1 flex-col" value="instances">
                                <AppInstancesTab
                                    app={app}
                                    canManageApp={canManage}
                                    canManageInstance={canManageInstance}
                                />
                            </TabsContent>

                            <TabsContent className="flex flex-1 flex-col" value="connections">
                                {renderInstanceTab((appInstance) => (
                                    <AppBindingsTab
                                        appInstanceId={appInstance.id}
                                        canManage={canManageInstance(appInstance)}
                                        key={appInstance.id}
                                    />
                                ))}
                            </TabsContent>

                            <TabsContent className="flex flex-1 flex-col" value="api-collections">
                                {renderInstanceTab((appInstance) => (
                                    <AppApiCollectionsTab
                                        appInstanceId={appInstance.id}
                                        canManage={canManageInstance(appInstance)}
                                        key={appInstance.id}
                                    />
                                ))}
                            </TabsContent>

                            <TabsContent className="flex flex-1 flex-col" value="workflows">
                                {renderInstanceTab((appInstance) => (
                                    <AppWorkflowsTab
                                        appInstanceId={appInstance.id}
                                        canManage={canManageInstance(appInstance)}
                                        key={appInstance.id}
                                    />
                                ))}
                            </TabsContent>

                            <TabsContent className="flex flex-1 flex-col" value="deployments">
                                {renderInstanceTab((appInstance) => (
                                    <AppDeploymentsTab
                                        appInstanceId={appInstance.id}
                                        canDeploy={canDeployInstance(appInstance)}
                                        deploymentConnectionId={appInstance.deploymentConnectionId}
                                        deploymentProviderAvailable={appInstance.deploymentProviderAvailable}
                                        key={appInstance.id}
                                    />
                                ))}
                            </TabsContent>

                            <TabsContent className="flex flex-1 flex-col" value="activity">
                                {renderInstanceTab((appInstance) => (
                                    <AppActivityTab appId={appId} appInstance={appInstance} key={appInstance.id} />
                                ))}
                            </TabsContent>
                        </Tabs>

                        {showDeleteDialog && (
                            <AppDeleteDialog
                                app={app}
                                onClose={() => setShowDeleteDialog(false)}
                                onDeleted={() => navigate('/automation/apps')}
                                open={showDeleteDialog}
                            />
                        )}
                    </div>
                )}
            </PageLoader>

            {canManage && <AppDialog onOpenChange={setShowCreateDialog} open={showCreateDialog} />}
        </LayoutContainer>
    );
};

export default AppDetail;
