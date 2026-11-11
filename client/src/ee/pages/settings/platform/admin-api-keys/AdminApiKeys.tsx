import {ADMIN_API_KEYS_DOCUMENTATION_URL} from '@/pages/settings/settingsDocumentationUrls';
import ApiKeysContent from '@/shared/components/api-keys/ApiKeysContent';
import {ApiKeysProvider} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {getApiKeyMutationHooks} from '@/shared/components/api-keys/utils/getApiKeyMutationHooks';
import {ApiKey, useAdminApiKeysQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseQueryResult, useQueryClient} from '@tanstack/react-query';

const AdminApiKeys = () => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    return (
        <ApiKeysProvider
            value={{
                ...getApiKeyMutationHooks({environmentId, queryClient, queryKey: ['adminApiKeys']}),
                useApiKeysQuery: () => {
                    const {
                        data: {adminApiKeys} = {},
                        error,
                        isLoading,
                    } = useAdminApiKeysQuery({
                        environmentId: `${environmentId}`,
                    });

                    return {
                        data: adminApiKeys,
                        error,
                        isLoading,
                    } as UseQueryResult<ApiKey[], Error>;
                },
            }}
        >
            <ApiKeysContent
                description="Use Admin keys for programmatic administration of your account. Do not share your API key with others or expose it."
                docsLink={{href: ADMIN_API_KEYS_DOCUMENTATION_URL, label: 'How admin API keys work'}}
                emptyDescription="Scripts, CI pipelines and the management MCP server use an admin API key to administer this account without signing in."
                title="Admin API Keys"
            />
        </ApiKeysProvider>
    );
};

export default AdminApiKeys;
