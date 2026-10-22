import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface LeftSidebarStateI {
    projectLeftSidebarOpen: boolean;
    setProjectLeftSidebarOpen: (projectLeftSidebarStatus: boolean) => void;
}

const useProjectsLeftSidebarStore = create<LeftSidebarStateI>()(
    devtools((set) => ({
        projectLeftSidebarOpen: false,
        setProjectLeftSidebarOpen: (projectLeftSidebarOpen) =>
            set(() => ({
                projectLeftSidebarOpen,
            })),
    }))
);

export default useProjectsLeftSidebarStore;
