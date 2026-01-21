import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface EditUserDialogStateI {
    editRole: string | null;
    handleClose: () => void;
    handleOpen: (login: string) => void;
    handleRoleChange: (role: string) => void;
    loginToEdit: string | null;
}

export const useEditUserDialogStore = create<EditUserDialogStateI>()(
    devtools(
        (set) => ({
            editRole: null,

            handleClose: () => {
                set(() => ({
                    editRole: null,
                    loginToEdit: null,
                }));
            },

            handleOpen: (login: string) => {
                set(() => ({
                    loginToEdit: login,
                }));
            },

            handleRoleChange: (role: string) => {
                set(() => ({
                    editRole: role,
                }));
            },

            loginToEdit: null,
        }),
        {
            name: 'bytechef.edit-user-dialog',
        }
    )
);
