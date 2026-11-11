import {EMBEDDED_API_KEYS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import useEmbeddedApiKeysProviderValue from '@/ee/pages/settings/embedded/api-keys/hooks/useEmbeddedApiKeysProviderValue';
import ApiKeysContent from '@/shared/components/api-keys/ApiKeysContent';
import {ApiKeysProvider} from '@/shared/components/api-keys/providers/apiKeysProvider';

const ApiKeys = () => {
    const apiKeysProviderValue = useEmbeddedApiKeysProviderValue();

    return (
        <ApiKeysProvider value={apiKeysProviderValue}>
            <ApiKeysContent
                description="Do not share your API key with others or expose it in the browser or other client-side code."
                docsLink={{href: EMBEDDED_API_KEYS_DOCUMENTATION_URL, label: 'How API keys work'}}
                emptyDescription="Your backend sends an API key with every call to the Embedded API, such as listing a user's integrations or running an action."
                title="API Keys"
            />
        </ApiKeysProvider>
    );
};

export default ApiKeys;
