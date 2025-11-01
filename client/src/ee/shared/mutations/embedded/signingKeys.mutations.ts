import {CreateSigningKey200Response, SigningKey, SigningKeyApi} from '@/ee/shared/middleware/embedded/security';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMutation} from '@tanstack/react-query';

interface CreateSigningKeyMutationProps {
    onError?: (error: Error, variables: SigningKey) => void;
    onSuccess?: (result: CreateSigningKey200Response, variables: SigningKey) => void;
}

export const useCreateSigningKeyMutation = (mutationProps?: CreateSigningKeyMutationProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    return useMutation<CreateSigningKey200Response, Error, SigningKey>({
        mutationFn: (signingKey: SigningKey) => {
            return new SigningKeyApi().createSigningKey({
                signingKey: {
                    ...signingKey,
                    environmentId: currentEnvironmentId!,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

interface DeleteSigningKeyMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteSigningKeyMutation = (mutationProps?: DeleteSigningKeyMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new SigningKeyApi().deleteSigningKey({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateSigningKeyMutationProps {
    onError?: (error: Error, variables: SigningKey) => void;
    onSuccess?: (result: void, variables: SigningKey) => void;
}

export const useUpdateSigningKeyMutation = (mutationProps?: UpdateSigningKeyMutationProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    return useMutation<void, Error, SigningKey>({
        mutationFn: (signingKey: SigningKey) => {
            return new SigningKeyApi().updateSigningKey({
                id: signingKey.id!,
                signingKey: {
                    ...signingKey,
                    environmentId: currentEnvironmentId!,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};
