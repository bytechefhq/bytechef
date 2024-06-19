import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {useGetWorkspacesQuery} from '@/ee/shared/queries/automation/workspaces.queries';
import WorkspaceDialog from '@/pages/settings/automation/workspaces/components/WorkspaceDialog';
import WorkspaceList from '@/pages/settings/automation/workspaces/components/WorkspaceList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {ZapIcon} from 'lucide-react';

const Workspaces = () => {
    const {data: workspaces, error: workspacesError, isLoading: workspacesIsLoading} = useGetWorkspacesQuery();

    return (
        <PageLoader errors={[workspacesError]} loading={workspacesIsLoading}>
            <LayoutContainer
                header={
                    <Header
                        className="w-full px-4 2xl:mx-auto 2xl:w-4/5"
                        position="main"
                        right={
                            workspaces &&
                            workspaces.length > 0 && <WorkspaceDialog triggerNode={<Button>New Workspace</Button>} />
                        }
                        title="Workspaces"
                    />
                }
                leftSidebarOpen={false}
            >
                {workspaces && workspaces.length > 0 ? (
                    <WorkspaceList workspaces={workspaces} />
                ) : (
                    <EmptyList
                        button={<WorkspaceDialog triggerNode={<Button>New App Event</Button>} />}
                        icon={<ZapIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new app event."
                        title="No App Events"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default Workspaces;
