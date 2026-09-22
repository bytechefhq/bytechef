import {create} from 'zustand';

type StateContributorType = () => Record<string, unknown>;

interface StateContributorRegistryStateI {
    contribute: () => Record<string, unknown>;
    contributors: StateContributorType[];
    register: (contributor: StateContributorType) => () => void;
}

const useCopilotStateContributorRegistry = create<StateContributorRegistryStateI>((set, get) => ({
    contribute: () =>
        get().contributors.reduce<Record<string, unknown>>((contributedState, contributor) => {
            for (const [key, value] of Object.entries(contributor())) {
                if (value !== undefined) {
                    contributedState[key] = value;
                }
            }

            return contributedState;
        }, {}),
    contributors: [],
    register: (contributor) => {
        set((state) => ({contributors: [...state.contributors, contributor]}));

        return () => {
            set((state) => ({contributors: state.contributors.filter((entry) => entry !== contributor)}));
        };
    },
}));

export default useCopilotStateContributorRegistry;
