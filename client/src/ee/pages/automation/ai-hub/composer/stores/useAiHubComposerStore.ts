import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type ReferencedResourceKindType = 'aiAgent' | 'workflow';

export interface ReferencedResourceI {
    id: string;
    kind: ReferencedResourceKindType;
    name: string;
    ownsTab?: boolean;
    tabId?: string;
}

interface AiHubComposerStateI {
    referencedResources: ReferencedResourceI[];
    resourcePickerOpen: boolean;

    addReference: (resource: ReferencedResourceI) => void;
    clear: () => void;
    removeReference: (id: string, kind: ReferencedResourceKindType) => void;
    setResourcePickerOpen: (open: boolean) => void;
}

/* eslint-disable sort-keys */
export const aiHubComposerStore = create<AiHubComposerStateI>()(
    devtools((set) => ({
        referencedResources: [],
        resourcePickerOpen: false,

        addReference: (resource) =>
            set((state) => {
                const alreadyAdded = state.referencedResources.some(
                    (existingResource) => existingResource.id === resource.id && existingResource.kind === resource.kind
                );

                if (alreadyAdded) {
                    return state;
                }

                return {...state, referencedResources: [...state.referencedResources, resource]};
            }),

        clear: () => set((state) => ({...state, referencedResources: [], resourcePickerOpen: false})),

        removeReference: (id, kind) =>
            set((state) => ({
                ...state,
                referencedResources: state.referencedResources.filter(
                    (resource) => !(resource.id === id && resource.kind === kind)
                ),
            })),

        setResourcePickerOpen: (open) => set((state) => ({...state, resourcePickerOpen: open})),
    }))
);

export const useAiHubComposerStore = aiHubComposerStore;
