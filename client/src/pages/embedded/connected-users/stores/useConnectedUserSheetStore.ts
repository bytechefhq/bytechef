/* eslint-disable sort-keys */
import {create} from 'zustand';

interface ConnectedUserSheetStateI {
    connectedUserSheetOpen: boolean;
    setConnectedUserSheetOpen: (connectedUserSheetOpen: boolean) => void;

    connectedUserId: number;
    setConnectedUserId: (connectedUserId: number) => void;
}

export const useConnectedUserSheetStore = create<ConnectedUserSheetStateI>()((set) => ({
    connectedUserId: 0,
    setConnectedUserId: (connectedUserId) =>
        set((state) => ({
            ...state,
            connectedUserId: connectedUserId,
        })),

    connectedUserSheetOpen: false,
    setConnectedUserSheetOpen: (connectedUserSheetOpen) =>
        set((state) => ({
            ...state,
            connectedUserSheetOpen: connectedUserSheetOpen,
        })),
}));

export default useConnectedUserSheetStore;
