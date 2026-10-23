import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface InviteUserDialogStateI {
    inviteEmail: string;
    inviteRole: string | null;
    open: boolean;
    reset: () => void;
    setInviteEmail: (email: string) => void;
    setInviteRole: (role: string) => void;
    setOpen: () => void;
}

export const useInviteUserDialogStore = create<InviteUserDialogStateI>()(
    devtools(
        (set) => ({
            inviteEmail: '',
            inviteRole: null,
            open: false,

            reset: () => {
                set(() => ({
                    inviteEmail: '',
                    inviteRole: null,
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

            setOpen: () => {
                set(() => ({
                    open: true,
                }));
            },
        }),
        {
            name: 'bytechef.invite-user-dialog',
        }
    )
);
