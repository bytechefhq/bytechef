import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import WorkspaceDialog from '@/ee/pages/settings/automation/workspaces/components/WorkspaceDialog';
import WorkspaceList from '@/ee/pages/settings/automation/workspaces/components/WorkspaceList';
import {useGetWorkspacesQuery} from '@/ee/shared/queries/automation/workspaces.queries';
import {WORKSPACES_DOCUMENTATION_URL} from '@/pages/settings/settingsDocumentationUrls';
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
                        description="Manage organization workspaces: create, edit, or delete workspaces and their members."
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
                    <EmptyState
                        actions={<WorkspaceDialog triggerNode={<Button>New Workspace</Button>} />}
                        description="A workspace groups projects, connections, data tables and knowledge bases, and controls who can see them."
                        docsLink={{href: WORKSPACES_DOCUMENTATION_URL, label: 'How workspaces work'}}
                        icon={<ZapIcon />}
                        title="No Workspaces"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default Workspaces;
