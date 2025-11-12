import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ApiKeyDeleteDialog from '@/ee/shared/components/api-keys/components/ApiKeyDeleteDialog';
import ApiKeyDialog from '@/ee/shared/components/api-keys/components/ApiKeyDialog';
import ApiKeyTable from '@/ee/shared/components/api-keys/components/ApiKeyTable';
import useApiKeys from '@/ee/shared/components/api-keys/hooks/useApiKeys';
import {useApiKeysStore} from '@/ee/shared/components/api-keys/stores/useApiKeysStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {KeyIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

const ApiKeysContent = ({description, title}: {description: string; title: string}) => {
    const {setShowEditDialog, showDeleteDialog, showEditDialog} = useApiKeysStore(
        useShallow((state) => ({
            setShowEditDialog: state.setShowEditDialog,
            showDeleteDialog: state.showDeleteDialog,
            showEditDialog: state.showEditDialog,
        }))
    );

    const {apiKeys, apiKeysError, apiKeysLoading} = useApiKeys();

    return (
        <PageLoader errors={[apiKeysError]} loading={apiKeysLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description={description}
                        position="main"
                        right={
                            apiKeys && apiKeys.length > 0 && <ApiKeyDialog triggerNode={<Button>New API Key</Button>} />
                        }
                        title={title}
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

                {showDeleteDialog && <ApiKeyDeleteDialog />}

                {showEditDialog && <ApiKeyDialog />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default ApiKeysContent;
