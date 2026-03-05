import {createContext, useContext} from 'react';

export interface ClusterElementContextI {
    clusterElementName: string;
    componentName: string;
    componentVersion: number;
    connectionId?: number;
    inputParameters: Record<string, unknown>;
}

const ClusterElementContext = createContext<ClusterElementContextI | null>(null);

export const ClusterElementProvider = ClusterElementContext.Provider;

export const useClusterElementContext = () => useContext(ClusterElementContext);
