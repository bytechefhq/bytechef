import {describe, expect, it} from 'vitest';

import {
    GRAPH_FRAME_HEADER_HEIGHT,
    GRAPH_FRAME_MIN_HEIGHT,
    GRAPH_FRAME_MIN_WIDTH,
    GRAPH_FRAME_PADDING,
    GRAPH_LOOP_GUTTER,
    GRAPH_MEMBER_BOX_WIDTH,
    GRAPH_START_SIZE,
    autoPlaceGraphMembers,
    computeGraphFrameSize,
    findFreeSpot,
    fromFrameChildPosition,
    getGraphStartPinnedBox,
    toFrameChildPosition,
} from './graphFrameGeometry';

describe('graphFrameGeometry', () => {
    describe('toFrameChildPosition / fromFrameChildPosition', () => {
        it('round-trips a position through the header offset', () => {
            const position = {x: 50, y: 20};

            expect(toFrameChildPosition(position)).toEqual({x: 50, y: 20 + GRAPH_FRAME_HEADER_HEIGHT});
            expect(fromFrameChildPosition(toFrameChildPosition(position))).toEqual(position);
        });
    });

    describe('computeGraphFrameSize', () => {
        it('pads the union of member boxes and honours minimums', () => {
            expect(computeGraphFrameSize([])).toEqual({height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH});
            expect(computeGraphFrameSize([{height: 100, name: 'a', width: 200, x: 500, y: 300}])).toEqual({
                height: 300 + 100 + GRAPH_FRAME_HEADER_HEIGHT + GRAPH_FRAME_PADDING,
                // Mirrors the 500 inset on the right, so the member sits centred.
                width: 500 + 200 + 500,
            });
        });

        it('mirrors the leftmost inset on the right so the member block is centred', () => {
            const size = computeGraphFrameSize([
                {height: 50, name: 'a', width: 100, x: 200, y: 0},
                {height: 50, name: 'b', width: 100, x: 400, y: 0},
            ]);

            const leftGap = 200;
            const rightGap = size.width - 500;

            expect(rightGap).toBe(leftGap);
        });

        it('falls back to the loop gutter when a member sits nearer the edge than that', () => {
            const size = computeGraphFrameSize([{height: 50, name: 'a', width: 100, x: 0, y: 0}]);

            expect(size.width).toBe(Math.max(GRAPH_FRAME_MIN_WIDTH, 100 + GRAPH_LOOP_GUTTER));
        });

        it('takes the union across several member boxes', () => {
            const size = computeGraphFrameSize([
                {height: 50, name: 'a', width: 100, x: 0, y: 0},
                {height: 50, name: 'b', width: 100, x: 400, y: 10},
            ]);

            expect(size).toEqual({
                height: Math.max(GRAPH_FRAME_MIN_HEIGHT, GRAPH_FRAME_HEADER_HEIGHT + 60 + GRAPH_FRAME_PADDING),
                // A member flush against the leading edge still gets the loop gutter mirrored.
                width: Math.max(GRAPH_FRAME_MIN_WIDTH, 500 + GRAPH_LOOP_GUTTER),
            });
        });
    });

    describe('findFreeSpot', () => {
        it('returns the start-marker offset when there are no members', () => {
            expect(findFreeSpot([])).toEqual({x: GRAPH_START_SIZE.width + 40, y: 0});
        });

        it('places to the right of the rightmost member', () => {
            expect(findFreeSpot([{height: 100, name: 'a', width: 200, x: 0, y: 0}])).toEqual({x: 200 + 60, y: 0});
        });

        it('ignores members off the top row', () => {
            const spot = findFreeSpot([
                {height: 100, name: 'a', width: 200, x: 0, y: 0},
                {height: 100, name: 'b', width: 500, x: 0, y: 400},
            ]);

            expect(spot).toEqual({x: 200 + 60, y: 0});
        });
    });

    describe('autoPlaceGraphMembers', () => {
        it('lays a chain down the flow axis below pinned members in TB', async () => {
            const positions = await autoPlaceGraphMembers(
                [
                    {height: 100, name: 'a', width: 200},
                    {height: 100, name: 'b', width: 200},
                ],
                [{from: 'a', to: 'b'}],
                [{height: 100, name: 'p', width: 200, x: 0, y: 0}]
            );

            expect(positions.a.y).toBeLessThan(positions.b.y);
            expect(positions.a.y).toBeGreaterThanOrEqual(100 + GRAPH_FRAME_PADDING);
        });

        it('lays a chain left-to-right past pinned members in LR', async () => {
            const positions = await autoPlaceGraphMembers(
                [
                    {height: 100, name: 'a', width: 200},
                    {height: 100, name: 'b', width: 200},
                ],
                [{from: 'a', to: 'b'}],
                [{height: 100, name: 'p', width: 200, x: 0, y: 0}],
                'LR'
            );

            expect(positions.a.x).toBeLessThan(positions.b.x);
            expect(positions.a.x).toBeGreaterThanOrEqual(200 + GRAPH_FRAME_PADDING);
        });

        it('offsets results by exactly the pinned-bottom difference between pinned states', async () => {
            const memberSizes = [{height: 100, name: 'a', width: 200}];

            const unpinnedPositions = await autoPlaceGraphMembers(memberSizes, [], []);
            const pinnedPositions = await autoPlaceGraphMembers(
                memberSizes,
                [],
                [{height: 100, name: 'p', width: 200, x: 0, y: 0}]
            );

            // ELK may add its own internal padding to an unconnected single node's raw y, so
            // this compares the two calls rather than asserting an absolute y — only the
            // DIFFERENCE (driven by pinnedBottom) is this module's own contract.
            expect(pinnedPositions.a.y - unpinnedPositions.a.y).toBe(100);
        });

        it('skips edges with a dynamic target and edges to unknown members', async () => {
            const positions = await autoPlaceGraphMembers(
                [
                    {height: 100, name: 'a', width: 200},
                    {height: 100, name: 'b', width: 200},
                ],
                [
                    {from: 'a', to: '=nextNode'},
                    {from: 'a', to: 'missing'},
                ],
                []
            );

            expect(Object.keys(positions).sort()).toEqual(['a', 'b']);
        });

        // The bug this pins: a member's label is measured with its node but hangs off the right of
        // the box, so a block auto-placed at a fixed gutter made the frame grow to fit the labels and
        // left everything visible sitting well left of the chain edge entering the frame at its
        // centre. Auto-place and frame sizing are two halves of one contract, so this exercises both.
        it.each([
            ['a long label', 168],
            ['no label overhang', 0],
        ])('centres the painted block in the frame it produces (%s)', async (_case, labelOverhang) => {
            const memberSizes = [
                {height: 72, labelOverhang, name: 'a', width: GRAPH_MEMBER_BOX_WIDTH},
                {height: 72, labelOverhang, name: 'b', width: GRAPH_MEMBER_BOX_WIDTH},
                {height: 72, labelOverhang, name: 'c', width: GRAPH_MEMBER_BOX_WIDTH},
            ];

            const positions = await autoPlaceGraphMembers(
                memberSizes,
                [
                    {from: 'a', to: 'b'},
                    {from: 'a', to: 'c'},
                ],
                [getGraphStartPinnedBox()]
            );

            const memberBoxes = memberSizes.map((memberSize) => ({
                height: memberSize.height,
                name: memberSize.name,
                paintedWidth: memberSize.width,
                width: memberSize.width + memberSize.labelOverhang,
                x: positions[memberSize.name].x,
                y: positions[memberSize.name].y,
            }));

            const size = computeGraphFrameSize(memberBoxes);

            const paintedLeft = Math.min(...memberBoxes.map((memberBox) => memberBox.x));
            const paintedRight = Math.max(...memberBoxes.map((memberBox) => memberBox.x + memberBox.paintedWidth));

            expect((paintedLeft + paintedRight) / 2).toBe(size.width / 2);

            // And the labels still fit inside the frame rather than being cut off by its border.
            expect(Math.max(...memberBoxes.map((memberBox) => memberBox.x + memberBox.width))).toBeLessThanOrEqual(
                size.width
            );
        });
    });
});
