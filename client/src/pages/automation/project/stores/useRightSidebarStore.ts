import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface RightSidebarState {
    rightSidebarOpen: boolean;
    setRightSidebarOpen: (rightSidebarStatus: boolean) => void;
}

const useRightSidebarStore = create<RightSidebarState>()(
    devtools(
        persist(
            (set) => ({
                rightSidebarOpen: false,
                setRightSidebarOpen: (rightSidebarOpen) =>
                    set(() => ({
                        rightSidebarOpen: rightSidebarOpen,
                    })),
            }),
            {
                name: 'right-sidebar',
            }
        )
    )
);

export default useRightSidebarStore;
