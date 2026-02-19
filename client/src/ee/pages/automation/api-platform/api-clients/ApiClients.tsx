import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ApiPlatformLeftSidebarNav from '@/ee/pages/automation/api-platform/ApiPlatformLeftSidebarNav';
import ApiClientDialog from '@/ee/pages/automation/api-platform/api-clients/components/ApiClientDialog';
import ApiClientTable from '@/ee/pages/automation/api-platform/api-clients/components/ApiClientTable';
import {useGetApiCollectionTagsQuery} from '@/ee/shared/mutations/automation/apiCollectionTags.queries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetApiClientsQuery} from '@/shared/queries/platform/apiClients.queries';
import {KeyIcon} from 'lucide-react';
import {useState} from 'react';

const ApiClients = () => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: apiKeys, error: apiKeysError, isLoading: apiKeysLoading} = useGetApiClientsQuery();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: true,
        id: currentWorkspaceId!,
        includeAllFields: false,
        projectDeployments: true,
    });

    const {data: tags} = useGetApiCollectionTagsQuery();

    return (
        <PageLoader errors={[apiKeysError]} loading={apiKeysLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        position="main"
                        right={
                            apiKeys &&
                            apiKeys.length > 0 && <ApiClientDialog triggerNode={<Button>New API Client</Button>} />
                        }
                        title="API Clients"
                    />
                }
                leftSidebarBody={<ApiPlatformLeftSidebarNav projects={projects} tags={tags} />}
                leftSidebarHeader={<Header title="API Collections" />}
                leftSidebarWidth="64"
            >
                {apiKeys && apiKeys.length > 0 ? (
                    <ApiClientTable apiClients={apiKeys} />
                ) : (
                    <EmptyList
                        button={<Button onClick={() => setShowEditDialog(true)}>New API Client</Button>}
                        icon={<KeyIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new API client."
                        title="No API Clients"
                    />
                )}

                {showEditDialog && <ApiClientDialog onClose={() => setShowEditDialog(false)} />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default ApiClients;
