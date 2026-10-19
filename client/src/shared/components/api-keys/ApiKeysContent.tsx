import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ApiKeyDeleteDialog from '@/shared/components/api-keys/components/ApiKeyDeleteDialog';
import ApiKeyDialog from '@/shared/components/api-keys/components/ApiKeyDialog';
import ApiKeyTable from '@/shared/components/api-keys/components/ApiKeyTable';
import useApiKeys from '@/shared/components/api-keys/hooks/useApiKeys';
import {useApiKeysProvider} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {useApiKeysStore} from '@/shared/components/api-keys/stores/useApiKeysStore';
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

    const {permissions} = useApiKeysProvider();

    const canCreate = permissions?.canCreate ?? true;
    const canManage = permissions?.canManage ?? true;
    const canView = permissions?.canView ?? true;

    return (
        <PageLoader errors={[apiKeysError]} loading={apiKeysLoading || !!permissions?.loading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description={description}
                        position="main"
                        right={
                            canCreate &&
                            apiKeys &&
                            apiKeys.length > 0 && <ApiKeyDialog triggerNode={<Button>New API Key</Button>} />
                        }
                        title={title}
                    />
                }
                leftSidebarOpen={false}
            >
                {!canView ? (
                    <EmptyList
                        icon={<KeyIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Only workspace editors can create and manage API keys."
                        title="No Access"
                    />
                ) : apiKeys && apiKeys.length > 0 ? (
                    <ApiKeyTable apiKeys={apiKeys} canManage={canManage} />
                ) : (
                    <EmptyList
                        button={
                            canCreate ? <Button onClick={() => setShowEditDialog(true)}>New API Key</Button> : undefined
                        }
                        icon={<KeyIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message={canCreate ? 'Get started by creating a new API key.' : undefined}
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
