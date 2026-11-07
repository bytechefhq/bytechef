import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type ReferencedResourceKindType = 'aiAgent' | 'file' | 'workflow';

export interface ReferencedResourceI {
    id: string;
    kind: ReferencedResourceKindType;
    name: string;
    ownsTab?: boolean;
    tabId?: string;
}

export interface SelectedSkillI {
    id: string;
    name: string;
}

interface AiHubComposerStateI {
    referencedResources: ReferencedResourceI[];
    resourcePickerOpen: boolean;
    selectedSkills: SelectedSkillI[];

    addReference: (resource: ReferencedResourceI) => void;
    addSkill: (skill: SelectedSkillI) => void;
    clear: () => void;
    removeReference: (id: string, kind: ReferencedResourceKindType) => void;
    removeSkill: (id: string) => void;
    setResourcePickerOpen: (open: boolean) => void;
}

/* eslint-disable sort-keys */
export const aiHubComposerStore = create<AiHubComposerStateI>()(
    devtools((set) => ({
        referencedResources: [],
        resourcePickerOpen: false,
        selectedSkills: [],

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

        addSkill: (skill) =>
            set((state) => {
                const alreadyAdded = state.selectedSkills.some((existingSkill) => existingSkill.id === skill.id);

                if (alreadyAdded) {
                    return state;
                }

                return {...state, selectedSkills: [...state.selectedSkills, skill]};
            }),

        clear: () =>
            set((state) => ({...state, referencedResources: [], resourcePickerOpen: false, selectedSkills: []})),

        removeReference: (id, kind) =>
            set((state) => ({
                ...state,
                referencedResources: state.referencedResources.filter(
                    (resource) => !(resource.id === id && resource.kind === kind)
                ),
            })),

        removeSkill: (id) =>
            set((state) => ({
                ...state,
                selectedSkills: state.selectedSkills.filter((skill) => skill.id !== id),
            })),

        setResourcePickerOpen: (open) => set((state) => ({...state, resourcePickerOpen: open})),
    }))
);

export const useAiHubComposerStore = aiHubComposerStore;
