import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import WorkspaceDialog from '@/pages/platform/settings/automation/workspaces/components/WorkspaceDialog';
import WorkspaceList from '@/pages/platform/settings/automation/workspaces/components/WorkspaceList';
import {useGetWorkspacesQuery} from '@/queries/automation/workspaces.queries';
import {ZapIcon} from 'lucide-react';

const Workspaces = () => {
    const {data: workspaces, error: workspacesError, isLoading: workspacesIsLoading} = useGetWorkspacesQuery();

    return (
        <PageLoader errors={[workspacesError]} loading={workspacesIsLoading}>
            <LayoutContainer
                header={
                    <PageHeader
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
                        icon={<ZapIcon className="size-12 text-gray-400" />}
                        message="Get started by creating a new app event."
                        title="No App Events"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default Workspaces;
