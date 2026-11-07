import {XYPosition} from '@xyflow/react';

export type AutoPlacedGraphPositionsRefType = {current: Record<string, Record<string, XYPosition>>};

let registeredRef: AutoPlacedGraphPositionsRefType | undefined;

export function registerAutoPlacedGraphPositions(autoPlacedPositionsRef: AutoPlacedGraphPositionsRefType): () => void {
    registeredRef = autoPlacedPositionsRef;

    return () => {
        if (registeredRef === autoPlacedPositionsRef) {
            registeredRef = undefined;
        }
    };
}

export function takeAutoPlacedGraphPositions(graphId: string): Record<string, XYPosition> | undefined {
    if (!registeredRef) {
        return undefined;
    }

    const positions = registeredRef.current[graphId];

    delete registeredRef.current[graphId];

    return positions;
}
