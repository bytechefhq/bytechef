import {WorkspaceAssignmentInput} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface InviteUserDialogStateI {
    inviteEmail: string;
    inviteRole: string | null;
    inviteWorkspaces: WorkspaceAssignmentInput[];
    open: boolean;
    removeInviteWorkspace: (workspaceId: string) => void;
    reset: () => void;
    setInviteEmail: (email: string) => void;
    setInviteRole: (role: string) => void;
    setInviteWorkspaceRole: (workspaceId: string, roleName: string) => void;
    setOpen: () => void;
    toggleInviteWorkspace: (workspaceId: string, roleName: string) => void;
}

export const useInviteUserDialogStore = create<InviteUserDialogStateI>()(
    devtools(
        (set) => ({
            inviteEmail: '',
            inviteRole: null,
            inviteWorkspaces: [],
            open: false,

            removeInviteWorkspace: (workspaceId: string) => {
                set((state) => ({
                    inviteWorkspaces: state.inviteWorkspaces.filter(
                        (workspace) => workspace.workspaceId !== workspaceId
                    ),
                }));
            },

            reset: () => {
                set(() => ({
                    inviteEmail: '',
                    inviteRole: null,
                    inviteWorkspaces: [],
                    open: false,
                }));
            },

            setInviteEmail: (email: string) => {
                set(() => ({
                    inviteEmail: email,
                }));
            },

            setInviteRole: (role: string) => {
                set(() => ({
                    inviteRole: role,
                }));
            },

            setInviteWorkspaceRole: (workspaceId: string, roleName: string) => {
                set((state) => ({
                    inviteWorkspaces: state.inviteWorkspaces.map((workspace) =>
                        workspace.workspaceId === workspaceId ? {...workspace, roleName} : workspace
                    ),
                }));
            },

            setOpen: () => {
                set(() => ({
                    open: true,
                }));
            },

            toggleInviteWorkspace: (workspaceId: string, roleName: string) => {
                set((state) => {
                    const selected = state.inviteWorkspaces.some((workspace) => workspace.workspaceId === workspaceId);

                    return {
                        inviteWorkspaces: selected
                            ? state.inviteWorkspaces.filter((workspace) => workspace.workspaceId !== workspaceId)
                            : [...state.inviteWorkspaces, {roleName, workspaceId}],
                    };
                });
            },
        }),
        {
            name: 'bytechef.invite-user-dialog',
        }
    )
);
