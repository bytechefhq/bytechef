import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import WorkspaceDialog from '@/ee/pages/settings/automation/workspaces/components/WorkspaceDialog';
import WorkspaceList from '@/ee/pages/settings/automation/workspaces/components/WorkspaceList';
import {useGetWorkspacesQuery} from '@/ee/shared/queries/automation/workspaces.queries';
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
                        centerTitle
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
                        button={<WorkspaceDialog triggerNode={<Button>New Workspace</Button>} />}
                        icon={<ZapIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a workspace."
                        title="No Workspaces"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default Workspaces;
