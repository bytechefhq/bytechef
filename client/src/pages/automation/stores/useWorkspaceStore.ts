import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface WorkspaceStateI {
    currentWorkspaceId: number | undefined;
    setCurrentWorkspaceId: (currentWorkspaceId: number) => void;
}

export const useWorkspaceStore = create<WorkspaceStateI>()(
    devtools(
        persist(
            (set) => ({
                currentWorkspaceId: undefined,
                setCurrentWorkspaceId: (currentWorkspaceId) =>
                    set(() => ({
                        currentWorkspaceId,
                    })),
            }),
            {
                name: 'current-workspace',
            }
        )
    )
);
