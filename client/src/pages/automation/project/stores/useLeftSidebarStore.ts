import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface LeftSidebarStateI {
    leftSidebarOpen: boolean;
    setLeftSidebarOpen: (leftSidebarStatus: boolean) => void;
}

const useLeftSidebarStore = create<LeftSidebarStateI>()(
    devtools(
        (set) => ({
            leftSidebarOpen: false,
            setLeftSidebarOpen: (leftSidebarOpen) =>
                set(() => ({
                    leftSidebarOpen,
                })),
        }),
        {
            name: 'left-sidebar',
        }
    )
);

export default useLeftSidebarStore;
