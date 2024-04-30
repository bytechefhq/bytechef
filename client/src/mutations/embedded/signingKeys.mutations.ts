import {type CreateSigningKey200ResponseModel, SigningKeyApi, SigningKeyModel} from '@/middleware/embedded/user';
import {useMutation} from '@tanstack/react-query';

interface CreateSigningKeyMutationProps {
    onError?: (error: Error, variables: SigningKeyModel) => void;
    onSuccess?: (result: CreateSigningKey200ResponseModel, variables: SigningKeyModel) => void;
}

export const useCreateSigningKeyMutation = (mutationProps?: CreateSigningKeyMutationProps) =>
    useMutation<CreateSigningKey200ResponseModel, Error, SigningKeyModel>({
        mutationFn: (signingKeyModel: SigningKeyModel) => {
            return new SigningKeyApi().createSigningKey({
                signingKeyModel,
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
    onError?: (error: Error, variables: SigningKeyModel) => void;
    onSuccess?: (result: SigningKeyModel, variables: SigningKeyModel) => void;
}

export const useUpdateSigningKeyMutation = (mutationProps?: UpdateSigningKeyMutationProps) =>
    useMutation<SigningKeyModel, Error, SigningKeyModel>({
        mutationFn: (signingKeyModel: SigningKeyModel) => {
            return new SigningKeyApi().updateSigningKey({
                id: signingKeyModel.id!,
                signingKeyModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
