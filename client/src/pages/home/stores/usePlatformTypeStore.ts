import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const enum PlatformType {
    AUTOMATION,
    EMBEDDED,
}

interface PlatformTypeI {
    currentType: PlatformType | undefined;
    setCurrentType: (currentType: PlatformType) => void;
}

export const usePlatformTypeStore = create<PlatformTypeI>()(
    devtools(
        persist(
            (set) => ({
                currentType: PlatformType.AUTOMATION,
                setCurrentType: (currentType) =>
                    set(() => ({
                        currentType,
                    })),
            }),
            {
                name: 'bytechef.mode-type',
            }
        )
    )
);
