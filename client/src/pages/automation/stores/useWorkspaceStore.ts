import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface WorkspaceStateI {
    clearCurrentWorkspaceId: () => void;

    currentWorkspaceId: number;
    setCurrentWorkspaceId: (currentWorkspaceId: number) => void;
}

export const useWorkspaceStore = create<WorkspaceStateI>()(
    devtools(
        persist(
            (set) => ({
                clearCurrentWorkspaceId: () => {
                    set(() => ({
                        currentWorkspaceId: undefined,
                    }));
                },

                currentWorkspaceId: 1049, // Default workspace id,
                setCurrentWorkspaceId: (currentWorkspaceId: number) =>
                    set(() => ({
                        currentWorkspaceId,
                    })),
            }),
            {
                name: 'bytechef.workspace',
            }
        )
    )
);
