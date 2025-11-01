import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';

/* eslint-disable sort-keys */

import {Environment} from '@/shared/middleware/platform/configuration';
import {createStore, useStore} from 'zustand';
import {ExtractState} from 'zustand/index';
import {devtools, persist} from 'zustand/middleware';

interface EnvironmentStateI {
    currentEnvironmentId: number;
    clearCurrentEnvironmentId: () => void;
    setCurrentEnvironmentId: (currentEnvironmentId: number) => void;

    environments: Environment[];
    setEnvironments: (environments: Environment[]) => void;
}

export const environmentStore = createStore<EnvironmentStateI>()(
    devtools(
        persist(
            (set) => ({
                currentEnvironmentId: DEVELOPMENT_ENVIRONMENT,
                clearCurrentEnvironmentId: () => {
                    set(() => ({
                        currentEnvironmentId: DEVELOPMENT_ENVIRONMENT,
                    }));
                },
                setCurrentEnvironmentId: (currentEnvironmentId: number) =>
                    set(() => ({
                        currentEnvironmentId,
                    })),

                environments: [],
                setEnvironments: (environments: Environment[]) =>
                    set(() => ({
                        environments,
                    })),
            }),
            {
                name: 'bytechef.environment',
            }
        )
    )
);

export function useEnvironmentStore<U>(selector: (state: ExtractState<typeof environmentStore>) => U): U {
    return useStore(environmentStore, selector);
}
