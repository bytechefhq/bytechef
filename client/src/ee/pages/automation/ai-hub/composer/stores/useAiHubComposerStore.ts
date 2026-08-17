import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type ReferencedResourceKindType =
    'apiCollection' | 'dataTable' | 'file' | 'knowledgeBase' | 'mcpServer' | 'chat' | 'workflow' | 'workflowExecution';

export interface ReferencedResourceI {
    id: string;
    kind: ReferencedResourceKindType;
    name: string;
}

/**
 * A skill picked through the composer's '/' menu. Unlike a referenced resource, a skill isn't sent as
 * agent state — it is prefixed onto the outgoing message as `/<name>` (see AiHubRuntimeProvider.onNew),
 * which is what the agent's prompt already teaches it to read. Holding the selection here instead of in
 * the composer text is what makes picking several skills possible, and lets each one render as a
 * removable chip rather than as raw text the user has to edit by hand.
 */
export interface SelectedSkillI {
    id: string;
    name: string;
}

interface AiHubComposerStateI {
    referencedResources: ReferencedResourceI[];
    selectedSkills: SelectedSkillI[];

    addReference: (resource: ReferencedResourceI) => void;
    addSkill: (skill: SelectedSkillI) => void;
    clear: () => void;
    removeReference: (id: string, kind: ReferencedResourceKindType) => void;
    removeSkill: (id: string) => void;
}

/* eslint-disable sort-keys */
export const aiHubComposerStore = create<AiHubComposerStateI>()(
    devtools((set) => ({
        referencedResources: [],
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

        clear: () => set((state) => ({...state, referencedResources: [], selectedSkills: []})),

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
    }))
);

export const useAiHubComposerStore = aiHubComposerStore;
