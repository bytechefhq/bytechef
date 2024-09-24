import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface ApiCollectionsEnabledEnabledStateI {
    apiCollectionMap: Map<number, boolean>;
    setApiCollectionEnabled: (projectInstanceId: number, enabled: boolean) => void;
}

export const useApiCollectionsEnabledStore = create<ApiCollectionsEnabledEnabledStateI>()(
    devtools(
        (set) => ({
            apiCollectionMap: new Map<number, boolean>(),
            setApiCollectionEnabled: (projectInstanceId, enabled) =>
                set(({apiCollectionMap}) => ({
                    apiCollectionMap: new Map<number, boolean>(apiCollectionMap.set(projectInstanceId, enabled)),
                })),
        }),
        {
            name: 'api-collections-enabled',
        }
    )
);
