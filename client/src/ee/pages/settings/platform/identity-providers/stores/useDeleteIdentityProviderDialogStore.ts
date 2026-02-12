import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteIdentityProviderDialogStateI {
    clearIdToDelete: () => void;
    idToDelete: string | null;
    setIdToDelete: (id: string | null) => void;
}

export const useDeleteIdentityProviderDialogStore = create<DeleteIdentityProviderDialogStateI>()(
    devtools(
        (set) => ({
            clearIdToDelete: () => {
                set(() => ({
                    idToDelete: null,
                }));
            },

            idToDelete: null,

            setIdToDelete: (id: string | null) => {
                set(() => ({
                    idToDelete: id,
                }));
            },
        }),
        {
            name: 'bytechef.delete-identity-provider-dialog',
        }
    )
);
