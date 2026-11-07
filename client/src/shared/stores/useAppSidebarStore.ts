import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AppSidebarStateI {
    open: boolean;
    setOpen: (open: boolean) => void;
}

const useAppSidebarStore = create<AppSidebarStateI>()(
    devtools(
        persist(
            (set) => ({
                open: false,

                setOpen: (open) => set({open}),
            }),
            {
                name: 'bytechef.app-sidebar',
            }
        )
    )
);

export default useAppSidebarStore;
