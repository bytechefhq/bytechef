import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AppSidebarStateI {
    open: boolean;
    setOpen: (open: boolean) => void;
}

/**
 * Remembers whether the main app sidebar is expanded or collapsed to the icon rail, so the choice
 * survives reloads and navigation instead of resetting on every page load.
 *
 * `SidebarProvider` keeps this state internally and only accepts a starting value, so persisting it
 * means driving the provider as a controlled component from here. Collapsed stays the default for a
 * first visit — the canvas benefits from the width.
 */
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
