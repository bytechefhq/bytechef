import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import AppActivityTab from '@/pages/automation/apps/components/AppActivityTab';
import AppApiCollectionsTab from '@/pages/automation/apps/components/AppApiCollectionsTab';
import AppBindingsTab from '@/pages/automation/apps/components/AppBindingsTab';
import AppDeleteDialog from '@/pages/automation/apps/components/AppDeleteDialog';
import AppDialog from '@/pages/automation/apps/components/AppDialog';
import AppOverviewTab from '@/pages/automation/apps/components/AppOverviewTab';
import AppRotateKeyDialog from '@/pages/automation/apps/components/AppRotateKeyDialog';
import AppStatusSwitch from '@/pages/automation/apps/components/AppStatusSwitch';
import AppWorkflowsTab from '@/pages/automation/apps/components/AppWorkflowsTab';
import {APP_MANAGE_SCOPE} from '@/pages/automation/apps/constants';
import useAppOwnerNames from '@/pages/automation/apps/hooks/useAppOwnerNames';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {App as AppType, useAppQuery, useMyWorkspaceScopesQuery} from '@/shared/middleware/graphql';
import {ActivityIcon, KeyRoundIcon, LinkIcon, NetworkIcon, TrashIcon, WorkflowIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const AppDetail = () => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showRotateDialog, setShowRotateDialog] = useState(false);

    const {id} = useParams<{id: string}>();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const getOwnerName = useAppOwnerNames();

    const navigate = useNavigate();

    const appId = id ?? '';

    const {data, error, isLoading} = useAppQuery({id: appId}, {enabled: !!appId});

    const {data: scopesData} = useMyWorkspaceScopesQuery({workspaceId: String(currentWorkspaceId)});

    const canManage = (scopesData?.myWorkspaceScopes ?? []).includes(APP_MANAGE_SCOPE);

    const app = data?.app as AppType | undefined;

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

                                <AppDialog app={app} triggerNode={<Button label="Edit" variant="outline" />} />

                                <Button
                                    icon={<KeyRoundIcon />}
                                    label="Rotate key"
                                    onClick={() => setShowRotateDialog(true)}
                                    variant="outline"
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
                        <Tabs className="flex-1" defaultValue="overview">
                            <div className="mb-4 flex items-center justify-between">
                                <TabsList>
                                    <TabsTrigger value="overview">Overview</TabsTrigger>

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

                                    <TabsTrigger value="activity">
                                        <ActivityIcon className="mr-2 size-4" />
                                        Activity
                                    </TabsTrigger>
                                </TabsList>
                            </div>

                            <TabsContent className="flex-1" value="overview">
                                <AppOverviewTab app={app} ownerName={getOwnerName(app.ownerUserId)} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="connections">
                                <AppBindingsTab app={app} canManage={canManage} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="api-collections">
                                <AppApiCollectionsTab app={app} canManage={canManage} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="workflows">
                                <AppWorkflowsTab app={app} canManage={canManage} />
                            </TabsContent>

                            <TabsContent className="flex-1" value="activity">
                                <AppActivityTab appId={appId} key={appId} />
                            </TabsContent>
                        </Tabs>

                        {showRotateDialog && (
                            <AppRotateKeyDialog
                                app={app}
                                onClose={() => setShowRotateDialog(false)}
                                open={showRotateDialog}
                            />
                        )}

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
