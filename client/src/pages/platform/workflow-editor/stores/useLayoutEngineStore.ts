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
                // The engine that produced the layout currently on the canvas. Selecting an
                // engine does not guarantee it runs: ELK falls back to dagre for unsupported
                // shapes (isElkLayoutSupported) and on layout errors, so geometry-coupled
                // renderers (the LR ring-bar handle flip on loop/each/map ghost bars) must key
                // on what actually ran, not on the selection.
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
