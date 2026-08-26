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
        const ref: AutoPlacedGraphPositionsRefType = {
            current: {graph_1: {task_1: {x: 10, y: 20}}, graph_2: {task_9: {x: 0, y: 0}}},
        };

        unregister = registerAutoPlacedGraphPositions(ref);

        expect(takeAutoPlacedGraphPositions('graph_1')).toEqual({task_1: {x: 10, y: 20}});
        expect(takeAutoPlacedGraphPositions('graph_1')).toBeUndefined();
        expect(ref.current).toEqual({graph_2: {task_9: {x: 0, y: 0}}});
    });

    // `useLayout` replaces `ref.current` on every pass, so the channel must read through the ref
    // rather than hold a snapshot of what it contained at registration time.
    it('reads through the ref after the layout replaces its contents', () => {
        const ref: AutoPlacedGraphPositionsRefType = {current: {}};

        unregister = registerAutoPlacedGraphPositions(ref);

        ref.current = {graph_1: {task_1: {x: 5, y: 5}}};

        expect(takeAutoPlacedGraphPositions('graph_1')).toEqual({task_1: {x: 5, y: 5}});
    });

    it('stops handing out positions once the canvas unregisters', () => {
        const ref: AutoPlacedGraphPositionsRefType = {current: {graph_1: {task_1: {x: 1, y: 2}}}};

        registerAutoPlacedGraphPositions(ref)();

        expect(takeAutoPlacedGraphPositions('graph_1')).toBeUndefined();
    });

    // A read-only canvas unmounting must not silently detach the editable canvas that replaced it.
    it('leaves a newer registration alone when an older one unregisters', () => {
        const firstRef: AutoPlacedGraphPositionsRefType = {current: {graph_1: {task_1: {x: 1, y: 1}}}};
        const secondRef: AutoPlacedGraphPositionsRefType = {current: {graph_1: {task_2: {x: 2, y: 2}}}};

        const unregisterFirst = registerAutoPlacedGraphPositions(firstRef);

        unregister = registerAutoPlacedGraphPositions(secondRef);

        unregisterFirst();

        expect(takeAutoPlacedGraphPositions('graph_1')).toEqual({task_2: {x: 2, y: 2}});
    });
});
