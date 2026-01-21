import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface EditUserDialogStateI {
    clearLoginToEdit: () => void;
    editRole: string | null;
    loginToEdit: string | null;
    setEditRole: (role: string) => void;
    setLoginToEdit: (login: string) => void;
}

export const useEditUserDialogStore = create<EditUserDialogStateI>()(
    devtools(
        (set) => ({
            clearLoginToEdit: () => {
                set(() => ({
                    editRole: null,
                    loginToEdit: null,
                }));
            },

            editRole: null,

            loginToEdit: null,

            setEditRole: (role: string) => {
                set(() => ({
                    editRole: role,
                }));
            },

            setLoginToEdit: (login: string) => {
                set(() => ({
                    loginToEdit: login,
                }));
            },
        }),
        {
            name: 'bytechef.edit-user-dialog',
        }
    )
);
