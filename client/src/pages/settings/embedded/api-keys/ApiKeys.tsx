import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ApiKeyDialog from '@/pages/settings/embedded/api-keys/components/ApiKeyDialog';
import ApiKeyTable from '@/pages/settings/embedded/api-keys/components/ApiKeyTable';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGeApiKeysQuery} from '@/shared/queries/embedded/apiKeys.queries';
import {KeyIcon} from 'lucide-react';
import {useState} from 'react';

const ApiKeys = () => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {data: apiKeys, error: apiKeysError, isLoading: apiKeysLoading} = useGeApiKeysQuery();

    return (
        <PageLoader errors={[apiKeysError]} loading={apiKeysLoading}>
            <LayoutContainer
                header={
                    <Header
                        className="w-full px-4 2xl:mx-auto 2xl:w-4/5"
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
