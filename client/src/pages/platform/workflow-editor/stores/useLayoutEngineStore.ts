import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export type LayoutEngineType = 'dagre' | 'elk';

interface LayoutEngineStateI {
    lastAppliedLayoutEngine: LayoutEngineType;
    layoutEngine: LayoutEngineType;
    setLastAppliedLayoutEngine: (lastAppliedLayoutEngine: LayoutEngineType) => void;
    setLayoutEngine: (layoutEngine: LayoutEngineType) => void;
}

/**
 * Selects the workflow editor layout engine. 'elk' is the default: it packs
 * band-aware with a strict no-crossing contract and supports every current
 * workflow shape (unsupported future shapes fall back to dagre per layout via
 * isElkLayoutSupported). 'dagre' remains selectable as a comparison engine.
 * Global (not per-workflow) on purpose — this is a development comparison switch.
 */
const useLayoutEngineStore = create<LayoutEngineStateI>()(
    devtools(
        persist(
            (set) => ({
                lastAppliedLayoutEngine: 'elk',

                layoutEngine: 'elk',

                setLastAppliedLayoutEngine: (lastAppliedLayoutEngine) => set({lastAppliedLayoutEngine}),

                setLayoutEngine: (layoutEngine) => set({layoutEngine}),
            }),
            {
                name: 'bytechef.layout-engine',
            }
        )
    )
);

export default useLayoutEngineStore;
