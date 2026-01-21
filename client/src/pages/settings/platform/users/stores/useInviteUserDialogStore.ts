import {generatePassword} from '@/pages/settings/platform/users/util/password-utils';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface InviteUserDialogStateI {
    handleClose: () => void;
    handleEmailChange: (email: string) => void;
    handleOpen: () => void;
    handleRegeneratePassword: () => void;
    handleRoleChange: (role: string) => void;
    inviteEmail: string;
    invitePassword: string;
    inviteRole: string | null;
    open: boolean;
}

export const useInviteUserDialogStore = create<InviteUserDialogStateI>()(
    devtools(
        (set) => ({
            handleClose: () => {
                set(() => ({
                    inviteEmail: '',
                    invitePassword: generatePassword(),
                    inviteRole: null,
                    open: false,
                }));
            },

            handleEmailChange: (email: string) => {
                set(() => ({
                    inviteEmail: email,
                }));
            },

            handleOpen: () => {
                set(() => ({
                    open: true,
                }));
            },

            handleRegeneratePassword: () => {
                set(() => ({
                    invitePassword: generatePassword(),
                }));
            },

            handleRoleChange: (role: string) => {
                set(() => ({
                    inviteRole: role,
                }));
            },

            inviteEmail: '',
            invitePassword: generatePassword(),
            inviteRole: null,
            open: false,
        }),
        {
            name: 'bytechef.invite-user-dialog',
        }
    )
);
