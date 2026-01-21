import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteUserDialogStateI {
    handleClose: () => void;
    handleOpen: (login: string | null) => void;
    loginToDelete: string | null;
}

export const useDeleteUserDialogStore = create<DeleteUserDialogStateI>()(
    devtools(
        (set) => ({
            handleClose: () => {
                set(() => ({
                    loginToDelete: null,
                }));
            },

            handleOpen: (login: string | null) => {
                set(() => ({
                    loginToDelete: login,
                }));
            },

            loginToDelete: null,
        }),
        {
            name: 'bytechef.delete-user-dialog',
        }
    )
);
