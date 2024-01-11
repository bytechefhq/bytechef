import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface IntegrationInstanceConfigurationsEnabledStateI {
    integrationInstanceConfigurationMap: Map<number, boolean>;
    setIntegrationInstanceConfigurationEnabled: (projectInstanceId: number, enabled: boolean) => void;
}

export const useIntegrationInstanceConfigurationsEnabledStore =
    create<IntegrationInstanceConfigurationsEnabledStateI>()(
        devtools(
            (set) => ({
                integrationInstanceConfigurationMap: new Map<number, boolean>(),
                setIntegrationInstanceConfigurationEnabled: (integrationInstanceConfigurationId, enabled) =>
                    set(({integrationInstanceConfigurationMap}) => ({
                        integrationInstanceConfigurationMap: new Map<number, boolean>(
                            integrationInstanceConfigurationMap.set(integrationInstanceConfigurationId, enabled)
                        ),
                    })),
            }),
            {
                name: 'integration-instance-configurations-enabled',
            }
        )
    );
