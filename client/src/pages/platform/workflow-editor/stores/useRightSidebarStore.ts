import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface RightSidebarStateI {
    rightSidebarOpen: boolean;
    setRightSidebarOpen: (rightSidebarStatus: boolean) => void;
}

const useRightSidebarStore = create<RightSidebarStateI>()(
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
                name: 'bytechef.right-sidebar',
            }
        )
    )
);

export default useRightSidebarStore;
