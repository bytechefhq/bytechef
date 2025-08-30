import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface EnvironmentStateI {
    clearCurrentEnvironmentId: () => void;

    currentEnvironmentId: number;
    setCurrentEnvironmentId: (currentEnvironmentId: number) => void;
}

export const useEnvironmentStore = create<EnvironmentStateI>()(
    devtools(
        persist(
            (set) => ({
                clearCurrentEnvironmentId: () => {
                    set(() => ({
                        currentEnvironmentId: undefined,
                    }));
                },

                currentEnvironmentId: 0,
                setCurrentEnvironmentId: (currentEnvironmentId: number) =>
                    set(() => ({
                        currentEnvironmentId: currentEnvironmentId,
                    })),
            }),
            {
                name: 'bytechef.environment',
            }
        )
    )
);
