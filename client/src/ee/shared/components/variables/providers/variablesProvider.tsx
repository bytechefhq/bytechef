import {Variable} from '@/shared/middleware/graphql';
import {ReactNode, createContext, useContext} from 'react';

export interface VariableFormValuesI {
    name: string;
    value: string;
}

export interface SimpleMutationProps {
    onSuccess?: (data: unknown) => void;
    onError?: (error: Error) => void;
}

export interface VariablesProviderStateI {
    canManage: boolean;
    useCreateVariableMutation: (props?: SimpleMutationProps) => {mutate: (input: VariableFormValuesI) => void};
    useDeleteVariableMutation: (props?: SimpleMutationProps) => {mutate: (input: {id: string}) => void};
    useUpdateVariableMutation: (props?: SimpleMutationProps) => {
        mutate: (input: {id: string} & VariableFormValuesI) => void;
    };
    useVariablesQuery: () => {data: Variable[] | undefined; error: Error | null; isLoading: boolean};
}

export interface VariablesProviderProps {
    children: ReactNode;
    value: VariablesProviderStateI;
}

const VariablesProviderContext = createContext<VariablesProviderStateI | undefined>(undefined);

export const VariablesProvider = ({children, value}: VariablesProviderProps) => {
    return <VariablesProviderContext.Provider value={value}>{children}</VariablesProviderContext.Provider>;
};

export const useVariablesProvider = (): VariablesProviderStateI => {
    const context = useContext(VariablesProviderContext);

    if (context === undefined) {
        throw new Error('useVariablesProvider must be used within a VariablesProvider');
    }

    return context;
};
