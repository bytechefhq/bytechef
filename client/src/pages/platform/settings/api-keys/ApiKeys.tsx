import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import ApiKeyDialog from '@/pages/platform/settings/api-keys/components/ApiKeyDialog';
import ApiKeyTable from '@/pages/platform/settings/api-keys/components/ApiKeyTable';
import {useGeApiKeysQuery} from '@/queries/platform/apiKeys.queries';
import {KeyIcon} from 'lucide-react';

const ApiKeys = () => {
    const {data: apiKeys, error: apiKeysError, isLoading: apiKeysLoading} = useGeApiKeysQuery();

    return (
        <PageLoader errors={[apiKeysError]} loading={apiKeysLoading}>
            <LayoutContainer
                header={
                    <PageHeader
                        className="w-full px-4 2xl:w-4/5"
                        position="main"
                        right={<ApiKeyDialog triggerNode={<Button>New API Key</Button>} />}
                        title="API Keys"
                    />
                }
                leftSidebarOpen={false}
            >
                {apiKeys && apiKeys.length > 0 ? (
                    <ApiKeyTable apiKeys={apiKeys} />
                ) : (
                    <EmptyList
                        icon={<KeyIcon className="size-12 text-gray-400" />}
                        message="Get started by creating a new API key."
                        title="No API Keys"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default ApiKeys;
