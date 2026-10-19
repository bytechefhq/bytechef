import {ApiKey} from '@/shared/middleware/graphql';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {ReactNode, createContext, useContext} from 'react';

export interface CreateApiKeyMutationProps {
    onSuccess?: (secretKey: string) => void;
    onError?: (error: Error) => void;
}

export interface SimpleMutationProps {
    onSuccess?: (data: unknown) => void;
    onError?: (error: Error) => void;
}

export interface ApiKeysProviderStateI {
    useApiKeysQuery: () => UseQueryResult<ApiKey[], Error>;
    useCreateApiKeyMutation: (props?: CreateApiKeyMutationProps) => UseMutationResult<unknown, Error>;
    useDeleteApiKeyMutation: (props?: SimpleMutationProps) => UseMutationResult<unknown, Error>;
    useUpdateApiKeyMutation: (props?: SimpleMutationProps) => UseMutationResult<unknown, Error>;
}

export interface ApiKeysProviderProps {
    children: ReactNode;
    value: ApiKeysProviderStateI;
}

const ApiKeysProviderContext = createContext<ApiKeysProviderStateI | undefined>(undefined);

export const ApiKeysProvider = ({children, value}: ApiKeysProviderProps) => {
    return <ApiKeysProviderContext.Provider value={value}>{children}</ApiKeysProviderContext.Provider>;
};

export const useApiKeysProvider = (): ApiKeysProviderStateI => {
    const context = useContext(ApiKeysProviderContext);

    if (context === undefined) {
        throw new Error('useApiKeysProvider must be used within a DefaultApiKeysProvider');
    }

    return context;
};
