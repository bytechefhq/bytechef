import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteUserDialogStateI {
    clearLoginToDelete: () => void;
    loginToDelete: string | null;
    setLoginToDelete: (login: string | null) => void;
}

export const useDeleteUserDialogStore = create<DeleteUserDialogStateI>()(
    devtools(
        (set) => ({
            clearLoginToDelete: () => {
                set(() => ({
                    loginToDelete: null,
                }));
            },

            loginToDelete: null,

            setLoginToDelete: (login: string | null) => {
                set(() => ({
                    loginToDelete: login,
                }));
            },
        }),
        {
            name: 'bytechef.delete-user-dialog',
        }
    )
);
