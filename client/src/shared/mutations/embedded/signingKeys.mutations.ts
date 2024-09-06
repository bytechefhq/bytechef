import {type CreateSigningKey200Response, SigningKey, SigningKeyApi} from '@/shared/middleware/embedded/user';
import {useMutation} from '@tanstack/react-query';

interface CreateSigningKeyMutationProps {
    onError?: (error: Error, variables: SigningKey) => void;
    onSuccess?: (result: CreateSigningKey200Response, variables: SigningKey) => void;
}

export const useCreateSigningKeyMutation = (mutationProps?: CreateSigningKeyMutationProps) =>
    useMutation<CreateSigningKey200Response, Error, SigningKey>({
        mutationFn: (signingKey: SigningKey) => {
            return new SigningKeyApi().createSigningKey({
                signingKey,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

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
    onSuccess?: (result: SigningKey, variables: SigningKey) => void;
}

export const useUpdateSigningKeyMutation = (mutationProps?: UpdateSigningKeyMutationProps) =>
    useMutation<SigningKey, Error, SigningKey>({
        mutationFn: (signingKey: SigningKey) => {
            return new SigningKeyApi().updateSigningKey({
                id: signingKey.id!,
                signingKey,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
