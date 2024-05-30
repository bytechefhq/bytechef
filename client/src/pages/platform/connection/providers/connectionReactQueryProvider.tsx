import {ConnectionModel, TagModel} from '@/shared/middleware/platform/connection';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {createContext, useContext} from 'react';

export interface RequestI {
    componentName?: string;
    connectionVersion?: number;
    tagId?: number;
}

export interface ConnectionKeysI {
    connection: (id: number) => (string | number)[];
    connectionTags: string[];
    connections: string[];
    filteredConnections: (filters: RequestI) => (string | RequestI)[];
}

export interface CreateConnectionMutationProps {
    onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
    onError?: (error: Error, variables: ConnectionModel) => void;
}

export interface ConnectionQueryStateI {
    ConnectionKeys: ConnectionKeysI;
    useCreateConnectionMutation: (
        props?: CreateConnectionMutationProps
    ) => UseMutationResult<ConnectionModel, Error, ConnectionModel, unknown>;
    useGetConnectionTagsQuery: () => UseQueryResult<TagModel[], Error>;
    useGetConnectionsQuery: (request: RequestI, enabled?: boolean) => UseQueryResult<ConnectionModel[], Error>;
}

interface ConnectionReactQueryProviderProps {
    children: React.ReactNode;
    value: ConnectionQueryStateI;
}

const ConnectionQueryProviderContext = createContext<ConnectionQueryStateI | undefined>(undefined);

export const ConnectionReactQueryProvider = ({children, value}: ConnectionReactQueryProviderProps) => {
    return <ConnectionQueryProviderContext.Provider value={value}>{children}</ConnectionQueryProviderContext.Provider>;
};

export const useConnectionQuery = () => {
    const context = useContext(ConnectionQueryProviderContext);

    if (context === undefined) throw new Error('useConnectionQuery must be used within a ConnectionQueryProvider');

    return context;
};
