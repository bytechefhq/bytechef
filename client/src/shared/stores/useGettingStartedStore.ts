import {createStore, useStore} from 'zustand';
import {ExtractState} from 'zustand/index';
import {devtools, persist} from 'zustand/middleware';

export type GettingStartedCompletionType = 'COMPLETED' | 'DISMISSED';

export type GettingStartedPlatformType = 'automation' | 'embedded';

interface GettingStartedStateI {
    complete: (platform: GettingStartedPlatformType, completionType: GettingStartedCompletionType) => void;
    completions: Partial<Record<GettingStartedPlatformType, GettingStartedCompletionType>>;
}

export const gettingStartedStore = createStore<GettingStartedStateI>()(
    devtools(
        persist(
            (set) => ({
                complete: (platform, completionType) =>
                    set((state) =>
                        state.completions[platform]
                            ? state
                            : {completions: {...state.completions, [platform]: completionType}}
                    ),
                completions: {},
            }),
            {
                name: 'bytechef.getting-started',
                partialize: (state) => ({completions: state.completions}),
            }
        )
    )
);

export function isGettingStartedCompleted(platform: GettingStartedPlatformType): boolean {
    return gettingStartedStore.getState().completions[platform] !== undefined;
}

export function useGettingStartedStore<U>(selector: (state: ExtractState<typeof gettingStartedStore>) => U): U {
    return useStore(gettingStartedStore, selector);
}
