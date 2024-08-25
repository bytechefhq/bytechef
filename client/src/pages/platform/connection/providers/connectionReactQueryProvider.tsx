import {TagModel} from '@/shared/middleware/platform/connection';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {createContext, useContext} from 'react';

import type {ConnectionEnvironmentModel, CredentialStatusModel} from '@/shared/middleware/automation/connection';

export interface ConnectionModelI {
    readonly active?: boolean;
    authorizationName?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly authorizationParameters?: {[key: string]: any};
    componentName: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly connectionParameters?: {[key: string]: any};
    connectionVersion?: number;
    readonly createdBy?: string;
    readonly createdDate?: Date;
    credentialStatus?: CredentialStatusModel;
    environment?: ConnectionEnvironmentModel;
    readonly id?: number;
    readonly lastModifiedBy?: string;
    readonly lastModifiedDate?: Date;
    name: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters: {[key: string]: any};
    tags?: Array<TagModel>;
    version?: number;
    workspaceId?: number;
}

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
    onSuccess?: (result: ConnectionModelI, variables: ConnectionModelI) => void;
    onError?: (error: Error, variables: ConnectionModelI) => void;
}

export interface ConnectionQueryStateI {
    ConnectionKeys: ConnectionKeysI;
    useCreateConnectionMutation: (
        props?: CreateConnectionMutationProps
    ) => UseMutationResult<ConnectionModelI, Error, ConnectionModelI, unknown>;
    useGetConnectionTagsQuery: () => UseQueryResult<TagModel[], Error>;
    useGetConnectionsQuery: (request: RequestI, enabled?: boolean) => UseQueryResult<ConnectionModelI[], Error>;
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
