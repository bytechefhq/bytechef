import {EMBEDDED_API_KEYS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import useEmbeddedApiKeysProviderValue from '@/ee/pages/settings/embedded/api-keys/hooks/useEmbeddedApiKeysProviderValue';
import ApiKeysContent from '@/shared/components/api-keys/ApiKeysContent';
import {ApiKeysProvider} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const ApiKeys = () => {
    const canCreateApiKey = useHasEmbeddedScope('API_KEY_CREATE');
    const canDeleteApiKey = useHasEmbeddedScope('API_KEY_DELETE');
    const canViewApiKeys = useHasEmbeddedScope('API_KEY_VIEW');

    const apiKeysProviderValue = useEmbeddedApiKeysProviderValue();

    if (!canViewApiKeys) {
        return (
            <LayoutContainer header={<Header centerTitle position="main" title="API Keys" />} leftSidebarOpen={false}>
                <div className="p-4 text-sm text-muted-foreground">
                    You do not have permission to view the API keys of this environment.
                </div>
            </LayoutContainer>
        );
    }

    return (
        <ApiKeysProvider value={apiKeysProviderValue}>
            <ApiKeysContent
                canCreate={canCreateApiKey}
                canDelete={canDeleteApiKey}
                canEdit={canCreateApiKey}
                description="Do not share your API key with others or expose it in the browser or other client-side code."
                docsLink={{href: EMBEDDED_API_KEYS_DOCUMENTATION_URL, label: 'How API keys work'}}
                emptyDescription="Your backend sends an API key with every call to the Embedded API, such as listing a user's integrations or running an action."
                title="API Keys"
            />
        </ApiKeysProvider>
    );
};

export default ApiKeys;
