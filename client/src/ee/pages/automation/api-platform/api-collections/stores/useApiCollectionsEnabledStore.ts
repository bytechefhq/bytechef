import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface ApiCollectionsEnabledEnabledStateI {
    apiCollectionMap: Map<number, boolean>;
    setApiCollectionEnabled: (projectDeploymentId: number, enabled: boolean) => void;
}

export const useApiCollectionsEnabledStore = create<ApiCollectionsEnabledEnabledStateI>()(
    devtools(
        (set) => ({
            apiCollectionMap: new Map<number, boolean>(),
            setApiCollectionEnabled: (projectDeploymentId, enabled) =>
                set(({apiCollectionMap}) => ({
                    apiCollectionMap: new Map<number, boolean>(apiCollectionMap.set(projectDeploymentId, enabled)),
                })),
        }),
        {
            name: 'api-collections-enabled',
        }
    )
);
