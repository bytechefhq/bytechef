import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface UserGuidingStateI {
    launcherHidden: boolean;
    setLauncherHidden: (launcherHidden: boolean) => void;
}

export const useUserGuidingStore = create<UserGuidingStateI>()(
    devtools(
        persist(
            (set) => ({
                launcherHidden: false,

                setLauncherHidden: (launcherHidden) => set({launcherHidden}),
            }),
            {
                name: 'bytechef.user-guiding',
            }
        )
    )
);
