import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface LeftSidebarStateI {
    projectLeftSidebarOpen: boolean;
    setProjectLeftSidebarOpen: (projectLeftSidebarStatus: boolean) => void;
}

const useProjectsLeftSidebarStore = create<LeftSidebarStateI>()(
    devtools(
        persist(
            (set) => ({
                projectLeftSidebarOpen: false,
                setProjectLeftSidebarOpen: (projectLeftSidebarOpen) =>
                    set(() => ({
                        projectLeftSidebarOpen,
                    })),
            }),
            {
                name: 'bytechef.projects-left-sidebar',
            }
        )
    )
);

export default useProjectsLeftSidebarStore;
