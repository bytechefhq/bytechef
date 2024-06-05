import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface WorkspaceStateI {
    clearCurrentWorkspaceId: () => void;

    currentWorkspaceId: number | undefined;
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

                currentWorkspaceId: undefined,
                setCurrentWorkspaceId: (currentWorkspaceId: number) =>
                    set(() => ({
                        currentWorkspaceId,
                    })),
            }),
            {
                name: 'workspace',
            }
        )
    )
);
