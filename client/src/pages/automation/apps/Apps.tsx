import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import AppDialog from '@/pages/automation/apps/components/AppDialog';
import AppList from '@/pages/automation/apps/components/app-list/AppList';
import {APP_MANAGE_SCOPE} from '@/pages/automation/apps/constants';
import useAppOwnerNames from '@/pages/automation/apps/hooks/useAppOwnerNames';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {App, useAppsQuery, useMyWorkspaceScopesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {AppWindowIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

const Apps = () => {
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const environmentId = String(currentEnvironmentId);
    const workspaceId = String(currentWorkspaceId);

    const getOwnerName = useAppOwnerNames();

    const {data, error, isLoading} = useAppsQuery({environmentId, workspaceId});

    const {data: scopesData} = useMyWorkspaceScopesQuery({workspaceId});

    const canManage = (scopesData?.myWorkspaceScopes ?? []).includes(APP_MANAGE_SCOPE);

    const apps = useMemo(() => (data?.apps ?? []) as App[], [data?.apps]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        apps.length > 0 &&
                        canManage && <Button label="New App" onClick={() => setShowCreateDialog(true)} />
                    }
                    title="Apps"
                />
            }
        >
            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {apps.length > 0 ? (
                    <AppList apps={apps} canManage={canManage} getOwnerName={getOwnerName} />
                ) : (
                    <EmptyList
                        button={
                            canManage ? <Button label="New App" onClick={() => setShowCreateDialog(true)} /> : undefined
                        }
                        icon={<AppWindowIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Register an app to let it call connections through ByteChef."
                        title="No Apps"
                    />
                )}
            </PageLoader>

            {/* One instance for the whole page: it holds the one-time key, so it must outlive the switch between
                the empty state and the list that the first created app causes. */}

            {canManage && <AppDialog onOpenChange={setShowCreateDialog} open={showCreateDialog} />}
        </LayoutContainer>
    );
};

export default Apps;
