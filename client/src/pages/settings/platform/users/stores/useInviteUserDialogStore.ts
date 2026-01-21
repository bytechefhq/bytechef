import {generatePassword} from '@/pages/settings/platform/users/util/password-utils';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface InviteUserDialogStateI {
    inviteEmail: string;
    invitePassword: string;
    inviteRole: string | null;
    open: boolean;
    regeneratePassword: () => void;
    reset: () => void;
    setInviteEmail: (email: string) => void;
    setInviteRole: (role: string) => void;
    setOpen: () => void;
}

export const useInviteUserDialogStore = create<InviteUserDialogStateI>()(
    devtools(
        (set) => ({
            inviteEmail: '',
            invitePassword: generatePassword(),
            inviteRole: null,
            open: false,

            regeneratePassword: () => {
                set(() => ({
                    invitePassword: generatePassword(),
                }));
            },

            reset: () => {
                set(() => ({
                    inviteEmail: '',
                    invitePassword: generatePassword(),
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
