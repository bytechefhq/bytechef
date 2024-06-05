import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const enum Section {
    AUTOMATION,
    EMBEDDED,
}

interface WorkspaceStateI {
    currentSection: Section | undefined;
    setCurrentSection: (currentSection: Section) => void;
}

export const useSectionStore = create<WorkspaceStateI>()(
    devtools(
        persist(
            (set) => ({
                currentSection: undefined,
                setCurrentSection: (currentSection) =>
                    set(() => ({
                        currentSection,
                    })),
            }),
            {
                name: 'section',
            }
        )
    )
);
