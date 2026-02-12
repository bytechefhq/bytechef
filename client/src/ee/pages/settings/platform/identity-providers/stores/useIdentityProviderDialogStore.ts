import {IdentityProviderType} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface IdentityProviderDialogStateI {
    identityProvider: IdentityProviderType | null;
    open: boolean;
    reset: () => void;
    setIdentityProvider: (identityProvider: IdentityProviderType | null) => void;
    setOpen: (open: boolean) => void;
}

export const useIdentityProviderDialogStore = create<IdentityProviderDialogStateI>()(
    devtools(
        (set) => ({
            identityProvider: null,
            open: false,

            reset: () => {
                set(() => ({
                    identityProvider: null,
                    open: false,
                }));
            },

            setIdentityProvider: (identityProvider: IdentityProviderType | null) => {
                set(() => ({
                    identityProvider,
                }));
            },

            setOpen: (open: boolean) => {
                set(() => ({
                    open,
                }));
            },
        }),
        {
            name: 'bytechef.identity-provider-dialog',
        }
    )
);
