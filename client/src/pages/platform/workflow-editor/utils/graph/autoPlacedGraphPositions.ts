import {XYPosition} from '@xyflow/react';

/** graphId -> member name -> the content-origin position the layout pre-pass invented for it. */
export type AutoPlacedGraphPositionsRefType = {current: Record<string, Record<string, XYPosition>>};

let registeredRef: AutoPlacedGraphPositionsRefType | undefined;

/**
 * Publishes the layout pre-pass's pending auto-placed positions so every interaction that persists
 * a graph can flush them, including the ones that run outside React — a task is appended deep
 * inside `saveWorkflowDefinition`, which has no way to reach a hook's ref.
 *
 * The REF is registered, never a snapshot of its contents: `useLayout` replaces `ref.current`
 * wholesale on every pass, so a snapshot would go stale immediately while the ref stays live.
 *
 * Only the editable canvas registers, and it unregisters on unmount, so a read-only canvas
 * rendered alongside one cannot take the channel over. Two EDITABLE canvases mounted at once would
 * share it — there is only ever one today, and nothing but this comment enforces that.
 */
export function registerAutoPlacedGraphPositions(ref: AutoPlacedGraphPositionsRefType): () => void {
    registeredRef = ref;

    return () => {
        if (registeredRef === ref) {
            registeredRef = undefined;
        }
    };
}

/**
 * The graph's pending auto-placed positions, removed from the channel as they are handed out.
 *
 * Taking rather than peeking is what keeps one flush from happening twice: the caller is about to
 * write these into the definition, and anything the next layout still has to place itself is
 * reported afresh on that pass.
 */
export function takeAutoPlacedGraphPositions(graphId: string): Record<string, XYPosition> | undefined {
    if (!registeredRef) {
        return undefined;
    }

    const positions = registeredRef.current[graphId];

    delete registeredRef.current[graphId];

    return positions;
}
