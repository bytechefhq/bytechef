import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface PersistantIntegrationStore {
    leftSidebarOpen: boolean;
    setLeftSidebarOpen: (leftSidebarStatus: boolean) => void;
}

const useLeftSidebarStore = create<PersistantIntegrationStore>()(
    devtools(
        persist(
            (set) => ({
                leftSidebarOpen: false,
                setLeftSidebarOpen: (leftSidebarStatus) =>
                    set(() => ({
                        leftSidebarOpen: leftSidebarStatus,
                    })),
            }),
            {
                name: 'left-sidebar-open',
            }
        )
    )
);

export default useLeftSidebarStore;
