import Button from '@/components/Button/Button';
import StepDoneText from '@/components/GettingStarted/StepDoneText';
import useEmbeddedApiKeysProviderValue from '@/ee/pages/settings/embedded/api-keys/hooks/useEmbeddedApiKeysProviderValue';
import ApiKeyDialog from '@/shared/components/api-keys/components/ApiKeyDialog';
import {ApiKeysProvider, CreateApiKeyMutationProps} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {useApiKeysStore} from '@/shared/components/api-keys/stores/useApiKeysStore';
import {useQueryClient} from '@tanstack/react-query';

interface ApiKeyStepActionProps {
    apiKeyCreated: boolean;
}

const ApiKeyStepAction = ({apiKeyCreated}: ApiKeyStepActionProps) => {
    const secretKey = useApiKeysStore((state) => state.secretKey);

    const apiKeysProviderValue = useEmbeddedApiKeysProviderValue();

    const queryClient = useQueryClient();

    const providerValue = {
        ...apiKeysProviderValue,
        useCreateApiKeyMutation: (props?: CreateApiKeyMutationProps) =>
            apiKeysProviderValue.useCreateApiKeyMutation({
                ...props,
                onSuccess: (createdSecretKey: string) => {
                    void queryClient.invalidateQueries({queryKey: ['embeddedGettingStarted']});

                    props?.onSuccess?.(createdSecretKey);
                },
            }),
    };

    if (apiKeyCreated && !secretKey) {
        return <StepDoneText text="Key created" />;
    }

    return (
        <ApiKeysProvider value={providerValue}>
            <ApiKeyDialog triggerNode={<Button size="sm">Create API key</Button>} />
        </ApiKeysProvider>
    );
};

export default ApiKeyStepAction;
