import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import AdminApiKeyDialog from '@/pages/platform/settings/admin-api-keys/components/AdminApiKeyDialog';
import AdminApiKeyTable from '@/pages/platform/settings/admin-api-keys/components/AdminApiKeyTable';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetAdminApiKeysQuery} from '@/shared/queries/platform/adminApiKeys.queries';
import {KeyIcon} from 'lucide-react';
import {useState} from 'react';

const AdminApiKeys = () => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {data: adminApiKeys, error: adminApiKeysError, isLoading: adminApiKeysLoading} = useGetAdminApiKeysQuery();

    return (
        <PageLoader errors={[adminApiKeysError]} loading={adminApiKeysLoading}>
            <LayoutContainer
                header={
                    <Header
                        className="w-full px-4 2xl:mx-auto 2xl:w-4/5"
                        position="main"
                        right={
                            adminApiKeys &&
                            adminApiKeys.length > 0 && (
                                <AdminApiKeyDialog triggerNode={<Button>New Admin API Key</Button>} />
                            )
                        }
                        title="Admin API Keys"
                    />
                }
                leftSidebarOpen={false}
            >
                {adminApiKeys && adminApiKeys.length > 0 ? (
                    <AdminApiKeyTable adminApiKeys={adminApiKeys} />
                ) : (
                    <EmptyList
                        button={<Button onClick={() => setShowEditDialog(true)}>New Admin API Key</Button>}
                        icon={<KeyIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new Admin API key."
                        title="No Admin API Keys"
                    />
                )}

                {showEditDialog && <AdminApiKeyDialog onClose={() => setShowEditDialog(false)} />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default AdminApiKeys;
