import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
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
import AppOverviewTab from '@/pages/automation/apps/components/AppOverviewTab';
import AppStatusSwitch from '@/pages/automation/apps/components/AppStatusSwitch';
import AppWorkflowsTab from '@/pages/automation/apps/components/AppWorkflowsTab';
import {APP_MANAGE_SCOPE} from '@/pages/automation/apps/constants';
import useAppDeployScopes from '@/pages/automation/apps/hooks/useAppDeployScopes';
import useAppManageScopes from '@/pages/automation/apps/hooks/useAppManageScopes';
import useAppOwnerNames from '@/pages/automation/apps/hooks/useAppOwnerNames';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    App as AppType,
    AppInstance,
    AppInstanceStatus,
    useAppQuery,
    useMyWorkspaceScopesQuery,
} from '@/shared/middleware/graphql';
import {ActivityIcon, BoxesIcon, LinkIcon, NetworkIcon, RocketIcon, TrashIcon, WorkflowIcon} from 'lucide-react';
import {ReactNode, useCallback, useMemo, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const INSTANCE_TAB_VALUES = ['activity', 'api-collections', 'connections', 'deployments', 'workflows'];

const AppDetail = () => {
    const [activeTab, setActiveTab] = useState('overview');
    const [selectedAppInstanceId, setSelectedAppInstanceId] = useState<string | undefined>();
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {id} = useParams<{id: string}>();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const getOwnerName = useAppOwnerNames();

    const navigate = useNavigate();

    const appId = id ?? '';

    const {data, error, isLoading} = useAppQuery({id: appId}, {enabled: !!appId});

    const {data: scopesData} = useMyWorkspaceScopesQuery({workspaceId: String(currentWorkspaceId)});

    const canManage = (scopesData?.myWorkspaceScopes ?? []).includes(APP_MANAGE_SCOPE);

    const app = data?.app as AppType | undefined;

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
            <p className="py-8 text-center text-sm text-content-neutral-secondary">
                This app has no instance you can view in full.
            </p>
        );

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        app &&
                        canManage && (
                            <div className="flex items-center gap-2">
                                <AppStatusSwitch app={app} canManage={canManage} />

                                <AppDialog
                                    app={app}
                                    canShare={canManage}
                                    triggerNode={<Button label="Edit" variant="outline" />}
                                />

                                <Button
                                    icon={<TrashIcon />}
                                    label="Delete"
                                    onClick={() => setShowDeleteDialog(true)}
                                    variant="destructiveOutline"
                                />
                            </div>
                        )
                    }
                    title={app?.name || 'Loading...'}
                />
            }
        >
            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {app && (
                    <div className="mx-auto flex h-full w-full max-w-(--breakpoint-lg) flex-col p-4 pt-0">
                        <Tabs className="flex-1" onValueChange={setActiveTab} value={activeTab}>
                            <div className="mb-4 flex items-center justify-between">
                                <TabsList>
                                    <TabsTrigger value="overview">Overview</TabsTrigger>

                                    <TabsTrigger value="instances">
                                        <BoxesIcon className="mr-2 size-4" />
                                        Instances
                                    </TabsTrigger>

                                    <TabsTrigger value="connections">
                                        <LinkIcon className="mr-2 size-4" />
                                        Connections
                                    </TabsTrigger>

                                    <TabsTrigger value="api-collections">
                                        <NetworkIcon className="mr-2 size-4" />
                                        API Collections
                                    </TabsTrigger>

                                    <TabsTrigger value="workflows">
                                        <WorkflowIcon className="mr-2 size-4" />
                                        Workflows
                                    </TabsTrigger>

                                    <TabsTrigger value="deployments">
                                        <RocketIcon className="mr-2 size-4" />
                                        Deployments
                                    </TabsTrigger>

                                    <TabsTrigger value="activity">
                                        <ActivityIcon className="mr-2 size-4" />
                                        Activity
                                    </TabsTrigger>
                                </TabsList>

                                {INSTANCE_TAB_VALUES.includes(activeTab) && appInstances.length > 0 && (
                                    <AppInstanceSelect
                                        appInstances={appInstances}
                                        onValueChange={setSelectedAppInstanceId}
                                        value={selectedAppInstance?.id}
                                    />
                                )}
                            </div>

                            {INSTANCE_TAB_VALUES.includes(activeTab) && selectedAppInstance && (
                                <div className="mb-4">
                                    <AppDeploymentSummary
                                        appInstanceId={selectedAppInstance.id}
                                        key={selectedAppInstance.id}
                                    />
                                </div>
                            )}

                            <TabsContent className="flex-1" value="overview">
                                <AppOverviewTab app={app} ownerName={getOwnerName(app.ownerUserId)} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="instances">
                                <AppInstancesTab
                                    app={app}
                                    canManageApp={canManage}
                                    canManageInstance={canManageInstance}
                                />
                            </TabsContent>

                            <TabsContent className="flex-1" value="connections">
                                {renderInstanceTab((appInstance) => (
                                    <AppBindingsTab
                                        appInstanceId={appInstance.id}
                                        canManage={canManageInstance(appInstance)}
                                        key={appInstance.id}
                                    />
                                ))}
                            </TabsContent>

                            <TabsContent className="flex-1" value="api-collections">
                                {renderInstanceTab((appInstance) => (
                                    <AppApiCollectionsTab
                                        appInstanceId={appInstance.id}
                                        canManage={canManageInstance(appInstance)}
                                        key={appInstance.id}
                                    />
                                ))}
                            </TabsContent>

                            <TabsContent className="flex-1" value="workflows">
                                {renderInstanceTab((appInstance) => (
                                    <AppWorkflowsTab
                                        appInstanceId={appInstance.id}
                                        canManage={canManageInstance(appInstance)}
                                        key={appInstance.id}
                                    />
                                ))}
                            </TabsContent>

                            <TabsContent className="flex-1" value="deployments">
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

                            <TabsContent className="flex-1" value="activity">
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
        </LayoutContainer>
    );
};

export default AppDetail;
