import {ApiKey} from '@/shared/middleware/graphql';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ApiKeysStateI {
    currentApiKey: ApiKey | undefined;
    setCurrentApiKey: (currentApiKey: ApiKey | undefined) => void;

    showDeleteDialog: boolean;
    setShowDeleteDialog: (showDeleteDialog: boolean) => void;

    showEditDialog: boolean;
    setShowEditDialog: (showEditDialog: boolean) => void;

    secretKey: string | undefined;
    setSecretKey: (secretKey: string | undefined) => void;
}

export const useApiKeysStore = create<ApiKeysStateI>()(
    devtools(
        (set) => ({
            currentApiKey: undefined,
            setCurrentApiKey: (currentApiKey: ApiKey) =>
                set(() => ({
                    currentApiKey,
                })),

            showDeleteDialog: false,
            setShowDeleteDialog: (showDeleteDialog: boolean) =>
                set(() => ({
                    showDeleteDialog,
                })),

            showEditDialog: false,
            setShowEditDialog: (showEditDialog: boolean) =>
                set(() => ({
                    showEditDialog,
                })),

            secretKey: undefined,
            setSecretKey: (secretKey: string) =>
                set(() => ({
                    secretKey,
                })),
        }),
        {
            name: 'bytechef.api-keys',
        }
    )
);
