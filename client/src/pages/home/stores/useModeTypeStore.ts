import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const enum ModeType {
    AUTOMATION,
    EMBEDDED,
}

interface ModeTypeI {
    currentType: ModeType | undefined;
    setCurrentType: (currentType: ModeType) => void;
}

export const useModeTypeStore = create<ModeTypeI>()(
    devtools(
        persist(
            (set) => ({
                currentType: ModeType.AUTOMATION,
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
