import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export type LayoutEngineType = 'dagre' | 'elk';

interface LayoutEngineStateI {
    layoutEngine: LayoutEngineType;
    setLayoutEngine: (layoutEngine: LayoutEngineType) => void;
}

/**
 * Selects the workflow editor layout engine. 'elk' is the experimental engine
 * (phase 1: plain tasks + condition dispatchers only); 'dagre' is the default.
 * Global (not per-workflow) on purpose — this is a development comparison switch.
 */
const useLayoutEngineStore = create<LayoutEngineStateI>()(
    devtools(
        persist(
            (set) => ({
                layoutEngine: 'dagre',

                setLayoutEngine: (layoutEngine) => set({layoutEngine}),
            }),
            {
                name: 'bytechef.layout-engine',
            }
        )
    )
);

export default useLayoutEngineStore;
