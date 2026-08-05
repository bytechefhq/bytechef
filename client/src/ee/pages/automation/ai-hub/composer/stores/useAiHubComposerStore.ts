import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type ReferencedResourceKindType =
    'apiCollection' | 'dataTable' | 'file' | 'knowledgeBase' | 'mcpServer' | 'task' | 'workflow' | 'workflowExecution';

export interface ReferencedResourceI {
    id: string;
    kind: ReferencedResourceKindType;
    name: string;
}

interface AiHubComposerStateI {
    referencedResources: ReferencedResourceI[];

    addReference: (resource: ReferencedResourceI) => void;
    clear: () => void;
    removeReference: (id: string, kind: ReferencedResourceKindType) => void;
}

/* eslint-disable sort-keys */
export const aiHubComposerStore = create<AiHubComposerStateI>()(
    devtools((set) => ({
        referencedResources: [],

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

        clear: () => set((state) => ({...state, referencedResources: []})),

        removeReference: (id, kind) =>
            set((state) => ({
                ...state,
                referencedResources: state.referencedResources.filter(
                    (resource) => !(resource.id === id && resource.kind === kind)
                ),
            })),
    }))
);

export const useAiHubComposerStore = aiHubComposerStore;
