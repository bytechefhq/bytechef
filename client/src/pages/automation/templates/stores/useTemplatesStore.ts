/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface TemplatesI {
    category: string | undefined;
    setCategory: (category?: string) => void;

    query: string | undefined;
    setQuery: (query?: string) => void;
}

export const useTemplatesStore = create<TemplatesI>()(
    devtools(
        (set) => ({
            category: undefined,
            setCategory: (category) =>
                set(() => ({
                    category,
                })),

            query: undefined,
            setQuery: (query) =>
                set(() => ({
                    query,
                })),
        }),
        {
            name: 'bytechef.mode-type',
        }
    )
);
