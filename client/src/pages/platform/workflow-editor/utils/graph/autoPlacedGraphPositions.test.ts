import {afterEach, describe, expect, it} from 'vitest';

import {
    AutoPlacedGraphPositionsRefType,
    registerAutoPlacedGraphPositions,
    takeAutoPlacedGraphPositions,
} from './autoPlacedGraphPositions';

let unregister: (() => void) | undefined;

afterEach(() => {
    unregister?.();

    unregister = undefined;
});

describe('takeAutoPlacedGraphPositions', () => {
    it('returns nothing while no canvas is registered', () => {
        expect(takeAutoPlacedGraphPositions('graph_1')).toBeUndefined();
    });

    it('hands out one graph positions and removes them from the channel', () => {
        const autoPlacedPositionsRef: AutoPlacedGraphPositionsRefType = {
            current: {graph_1: {task_1: {x: 10, y: 20}}, graph_2: {task_9: {x: 0, y: 0}}},
        };

        unregister = registerAutoPlacedGraphPositions(autoPlacedPositionsRef);

        expect(takeAutoPlacedGraphPositions('graph_1')).toEqual({task_1: {x: 10, y: 20}});
        expect(takeAutoPlacedGraphPositions('graph_1')).toBeUndefined();
        expect(autoPlacedPositionsRef.current).toEqual({graph_2: {task_9: {x: 0, y: 0}}});
    });

    it('reads through the ref after the layout replaces its contents', () => {
        const autoPlacedPositionsRef: AutoPlacedGraphPositionsRefType = {current: {}};

        unregister = registerAutoPlacedGraphPositions(autoPlacedPositionsRef);

        autoPlacedPositionsRef.current = {graph_1: {task_1: {x: 5, y: 5}}};

        expect(takeAutoPlacedGraphPositions('graph_1')).toEqual({task_1: {x: 5, y: 5}});
    });

    it('stops handing out positions once the canvas unregisters', () => {
        const autoPlacedPositionsRef: AutoPlacedGraphPositionsRefType = {current: {graph_1: {task_1: {x: 1, y: 2}}}};

        registerAutoPlacedGraphPositions(autoPlacedPositionsRef)();

        expect(takeAutoPlacedGraphPositions('graph_1')).toBeUndefined();
    });

    it('leaves a newer registration alone when an older one unregisters', () => {
        const firstRef: AutoPlacedGraphPositionsRefType = {current: {graph_1: {task_1: {x: 1, y: 1}}}};
        const secondRef: AutoPlacedGraphPositionsRefType = {current: {graph_1: {task_2: {x: 2, y: 2}}}};

        const unregisterFirst = registerAutoPlacedGraphPositions(firstRef);

        unregister = registerAutoPlacedGraphPositions(secondRef);

        unregisterFirst();

        expect(takeAutoPlacedGraphPositions('graph_1')).toEqual({task_2: {x: 2, y: 2}});
    });
});
