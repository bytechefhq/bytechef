import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface LeftSidebarStateI {
    leftSidebarOpen: boolean;
    setLeftSidebarOpen: (rightSidebarStatus: boolean) => void;
}

const useProjectsLeftSidebarStore = create<LeftSidebarStateI>()(
    devtools(
        persist(
            (set) => ({
                leftSidebarOpen: false,
                setLeftSidebarOpen: (leftSidebarOpen) =>
                    set(() => ({
                        leftSidebarOpen,
                    })),
            }),
            {
                name: 'bytechef.projects-left-sidebar',
            }
        )
    )
);

export default useProjectsLeftSidebarStore;
