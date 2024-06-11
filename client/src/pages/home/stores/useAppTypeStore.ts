import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const enum AppType {
    AUTOMATION,
    EMBEDDED,
}

interface AppTypeI {
    currentType: AppType | undefined;
    setCurrentType: (currentType: AppType) => void;
}

export const useAppTypeStore = create<AppTypeI>()(
    devtools(
        persist(
            (set) => ({
                currentType: undefined,
                setCurrentType: (currentType) =>
                    set(() => ({
                        currentType,
                    })),
            }),
            {
                name: 'app-type',
            }
        )
    )
);
