import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import AppApiCollectionsTab from '@/pages/automation/apps/components/AppApiCollectionsTab';
import AppBindingsTab from '@/pages/automation/apps/components/AppBindingsTab';
import AppDeploymentSummary from '@/pages/automation/apps/components/AppDeploymentSummary';
import AppDeploymentsTab from '@/pages/automation/apps/components/AppDeploymentsTab';
import AppInstanceConfigurationDialog from '@/pages/automation/apps/components/AppInstanceConfigurationDialog';
import AppInstanceDeploymentSettingsDialog from '@/pages/automation/apps/components/AppInstanceDeploymentSettingsDialog';
import AppInstanceShareDialog from '@/pages/automation/apps/components/AppInstanceShareDialog';
import AppWorkflowsTab from '@/pages/automation/apps/components/AppWorkflowsTab';
import {APP_INSTANCE_STATUS_LABELS, APP_STATUS_LABELS, HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import useAppDeployScopes from '@/pages/automation/apps/hooks/useAppDeployScopes';
import useAppManageScopes from '@/pages/automation/apps/hooks/useAppManageScopes';
import {toEnvironmentName} from '@/shared/constants';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {AppInstance, AppInstanceStatus, useAppInstanceQuery} from '@/shared/middleware/graphql';
import {CloudUploadIcon, LinkIcon, NetworkIcon, RocketIcon, Settings2Icon, UsersIcon, WorkflowIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useParams} from 'react-router-dom';

const AppInstanceDetail = () => {
    const {id} = useParams<{id: string}>();

    const appInstanceId = id ?? '';

    const {data, error, isLoading} = useAppInstanceQuery({id: appInstanceId}, {enabled: !!appInstanceId});

    const appInstance = data?.appInstance as AppInstance | undefined;

    const manageTargets = useMemo(
        () => (appInstance ? [{environmentId: appInstance.environmentId, workspaceId: appInstance.workspaceId}] : []),
        [appInstance]
    );

    const canManageWorkspace = useAppManageScopes(manageTargets);
    const canDeployWorkspace = useAppDeployScopes(manageTargets);

    const canManage =
        !!appInstance &&
        appInstance.status !== AppInstanceStatus.Terminated &&
        canManageWorkspace(appInstance.workspaceId, appInstance.environmentId);

    const canDeploy =
        !!appInstance &&
        appInstance.status !== AppInstanceStatus.Terminated &&
        canDeployWorkspace(appInstance.workspaceId, appInstance.environmentId);

    const instanceRows = appInstance
        ? [
              {label: 'Status', value: APP_INSTANCE_STATUS_LABELS[appInstance.status]},
              {label: 'Environment', value: toEnvironmentName(Number(appInstance.environmentId))},
              {label: 'Hosting target', value: HOSTING_TARGET_LABELS[appInstance.hostingTarget]},
              {label: 'URL', value: appInstance.url || '-'},
              {label: 'Key hint', value: appInstance.secretKeyHint ? `…${appInstance.secretKeyHint}` : '-'},
          ]
        : [];

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        appInstance &&
                        canManage && (
                            <div className="flex items-center gap-2">
                                <AppInstanceConfigurationDialog
                                    appInstance={appInstance}
                                    triggerNode={
                                        <Button icon={<Settings2Icon />} label="Configure" variant="outline" />
                                    }
                                />

                                <AppInstanceDeploymentSettingsDialog
                                    appInstance={appInstance}
                                    triggerNode={
                                        <Button icon={<CloudUploadIcon />} label="Deployment" variant="outline" />
                                    }
                                />

                                <AppInstanceShareDialog
                                    appInstance={appInstance}
                                    triggerNode={<Button icon={<UsersIcon />} label="Share" variant="outline" />}
                                />
                            </div>
                        )
                    }
                    title={appInstance?.name || 'Loading...'}
                />
            }
        >
            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appInstance && (
                    <div className="mx-auto flex h-full w-full max-w-(--breakpoint-lg) flex-col gap-6 p-4 pt-0">
                        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                            <section aria-label="Instance" className="space-y-2">
                                <h2 className="text-sm font-semibold">Instance</h2>

                                <dl className="grid grid-cols-[max-content_1fr] gap-x-8 gap-y-2 text-sm">
                                    {instanceRows.map((instanceRow) => (
                                        <div className="contents" key={instanceRow.label}>
                                            <dt className="text-content-neutral-secondary">{instanceRow.label}</dt>

                                            <dd>{instanceRow.value}</dd>
                                        </div>
                                    ))}
                                </dl>
                            </section>

                            <section aria-label="App" className="space-y-2">
                                <h2 className="text-sm font-semibold">App</h2>

                                <dl className="grid grid-cols-[max-content_1fr] gap-x-8 gap-y-2 text-sm">
                                    <dt className="text-content-neutral-secondary">Name</dt>

                                    <dd>{appInstance.app.name}</dd>

                                    <dt className="text-content-neutral-secondary">Status</dt>

                                    <dd>{APP_STATUS_LABELS[appInstance.app.status]}</dd>

                                    {appInstance.app.description && (
                                        <>
                                            <dt className="text-content-neutral-secondary">Description</dt>

                                            <dd>{appInstance.app.description}</dd>
                                        </>
                                    )}
                                </dl>
                            </section>
                        </div>

                        <AppDeploymentSummary appInstanceId={appInstance.id} />

                        <Tabs className="flex-1" defaultValue="connections">
                            <TabsList className="mb-4">
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
                            </TabsList>

                            <TabsContent className="flex-1" value="connections">
                                <AppBindingsTab appInstanceId={appInstance.id} canManage={canManage} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="api-collections">
                                <AppApiCollectionsTab appInstanceId={appInstance.id} canManage={canManage} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="workflows">
                                <AppWorkflowsTab appInstanceId={appInstance.id} canManage={canManage} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="deployments">
                                <AppDeploymentsTab
                                    appInstanceId={appInstance.id}
                                    canDeploy={canDeploy}
                                    deploymentConnectionId={appInstance.deploymentConnectionId}
                                    deploymentProviderAvailable={appInstance.deploymentProviderAvailable}
                                />
                            </TabsContent>
                        </Tabs>
                    </div>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default AppInstanceDetail;
