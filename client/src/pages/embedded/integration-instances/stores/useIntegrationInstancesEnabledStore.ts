import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface IntegrationInstancesEnabledStateI {
    integrationInstanceMap: Map<number, boolean>;
    setIntegrationInstanceEnabled: (integrationInstanceId: number, enabled: boolean) => void;
}

export const useIntegrationInstancesEnabledStore = create<IntegrationInstancesEnabledStateI>()(
    devtools(
        (set) => ({
            integrationInstanceMap: new Map<number, boolean>(),
            setIntegrationInstanceEnabled: (integrationInstanceId, enabled) =>
                set(({integrationInstanceMap}) => ({
                    integrationInstanceMap: new Map<number, boolean>(
                        integrationInstanceMap.set(integrationInstanceId, enabled)
                    ),
                })),
        }),
        {
            name: 'integration-instances-enabled',
        }
    )
);
