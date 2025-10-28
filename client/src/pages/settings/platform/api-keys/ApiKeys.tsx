import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ApiKeyDialog from '@/pages/settings/platform/api-keys/components/ApiKeyDialog';
import ApiKeyTable from '@/pages/settings/platform/api-keys/components/ApiKeyTable';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetApiKeysQuery} from '@/shared/queries/platform/apiKeys.queries';
import {KeyIcon} from 'lucide-react';
import {useState} from 'react';

const ApiKeys = () => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {data: apiKeys, error: apiKeysError, isLoading: apiKeysLoading} = useGetApiKeysQuery();

    return (
        <PageLoader errors={[apiKeysError]} loading={apiKeysLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        position="main"
                        right={
                            apiKeys && apiKeys.length > 0 && <ApiKeyDialog triggerNode={<Button>New API Key</Button>} />
                        }
                        title="API Keys"
                    />
                }
                leftSidebarOpen={false}
            >
                {apiKeys && apiKeys.length > 0 ? (
                    <ApiKeyTable apiKeys={apiKeys} />
                ) : (
                    <EmptyList
                        button={<Button onClick={() => setShowEditDialog(true)}>New API Key</Button>}
                        icon={<KeyIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new API key."
                        title="No API Keys"
                    />
                )}

                {showEditDialog && <ApiKeyDialog onClose={() => setShowEditDialog(false)} />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default ApiKeys;
