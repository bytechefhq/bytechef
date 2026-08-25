import {Variable} from '@/shared/middleware/graphql';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface VariablesStateI {
    currentVariable: Variable | undefined;
    setCurrentVariable: (currentVariable: Variable | undefined) => void;

    showDeleteDialog: boolean;
    setShowDeleteDialog: (showDeleteDialog: boolean) => void;

    showEditDialog: boolean;
    setShowEditDialog: (showEditDialog: boolean) => void;
}

export const useVariablesStore = create<VariablesStateI>()(
    devtools(
        (set) => ({
            currentVariable: undefined,
            setCurrentVariable: (currentVariable: Variable | undefined) =>
                set(() => ({
                    currentVariable,
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
        }),
        {
            name: 'bytechef.variables',
        }
    )
);
