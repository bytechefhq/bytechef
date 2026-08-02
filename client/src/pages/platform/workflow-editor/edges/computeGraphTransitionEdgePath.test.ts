import {describe, expect, it} from 'vitest';

import computeGraphTransitionEdgePath from './computeGraphTransitionEdgePath';

describe('computeGraphTransitionEdgePath', () => {
    describe('self loop', () => {
        it('starts and ends at the same anchor point', () => {
            const {path} = computeGraphTransitionEdgePath({
                kind: 'self',
                offset: 0,
                sourceX: 100,
                sourceY: 200,
                targetX: 100,
                targetY: 200,
            });

            expect(path.startsWith('M 100,200')).toBe(true);
            expect(path.endsWith('100,200')).toBe(true);
        });

        it('bows above the anchor (the lane header) rather than below it', () => {
            const {labelY} = computeGraphTransitionEdgePath({
                kind: 'self',
                offset: 0,
                sourceX: 100,
                sourceY: 200,
                targetX: 100,
                targetY: 200,
            });

            expect(labelY).toBeLessThan(200);
        });

        it('widens the loop as the offset among stacked self-loops grows', () => {
            const first = computeGraphTransitionEdgePath({
                kind: 'self',
                offset: 0,
                sourceX: 100,
                sourceY: 200,
                targetX: 100,
                targetY: 200,
            });
            const second = computeGraphTransitionEdgePath({
                kind: 'self',
                offset: 1,
                sourceX: 100,
                sourceY: 200,
                targetX: 100,
                targetY: 200,
            });

            expect(second.path).not.toBe(first.path);
        });
    });

    describe('forward edges (horizontal spread, e.g. TB layout)', () => {
        it('bows above both endpoints', () => {
            const {labelY} = computeGraphTransitionEdgePath({
                kind: 'forward',
                offset: 0,
                sourceX: 0,
                sourceY: 300,
                targetX: 400,
                targetY: 300,
            });

            expect(labelY).toBeLessThan(300);
        });

        // Topological lane ordering makes `forward` the common arc kind — most arcs in a
        // well-ordered graph flow left to right — so unstacked forward arcs would leave a skip
        // arc and a short arc over the same band bowing the same flat amount and overlapping.
        it('stacks parallel forward-edges progressively further out as the offset grows', () => {
            const first = computeGraphTransitionEdgePath({
                kind: 'forward',
                offset: 0,
                sourceX: 0,
                sourceY: 300,
                targetX: 400,
                targetY: 300,
            });
            const second = computeGraphTransitionEdgePath({
                kind: 'forward',
                offset: 1,
                sourceX: 0,
                sourceY: 300,
                targetX: 400,
                targetY: 300,
            });

            expect(second.labelY).toBeLessThan(first.labelY);
        });
    });

    describe('back edges (horizontal spread, e.g. TB layout)', () => {
        it('bows further above the endpoints than a forward edge does', () => {
            const forward = computeGraphTransitionEdgePath({
                kind: 'forward',
                offset: 0,
                sourceX: 400,
                sourceY: 300,
                targetX: 0,
                targetY: 300,
            });
            const back = computeGraphTransitionEdgePath({
                kind: 'back',
                offset: 0,
                sourceX: 400,
                sourceY: 300,
                targetX: 0,
                targetY: 300,
            });

            expect(back.labelY).toBeLessThan(forward.labelY);
        });

        it('stacks parallel back-edges progressively further out as the offset grows', () => {
            const first = computeGraphTransitionEdgePath({
                kind: 'back',
                offset: 0,
                sourceX: 400,
                sourceY: 300,
                targetX: 0,
                targetY: 300,
            });
            const second = computeGraphTransitionEdgePath({
                kind: 'back',
                offset: 1,
                sourceX: 400,
                sourceY: 300,
                targetX: 0,
                targetY: 300,
            });

            expect(second.labelY).toBeLessThan(first.labelY);
        });
    });

    describe('shared band routing', () => {
        it('routes arcs between unlevel endpoints through the same band', () => {
            // A three-task lane's entry sits well above a single-task lane's, so anchoring to the
            // entries alone leaves arcs swooping between unlevel points.
            const fromTallLane = computeGraphTransitionEdgePath({
                bandY: 190,
                kind: 'forward',
                offset: 0,
                sourceX: 100,
                sourceY: 220,
                targetX: 400,
                targetY: 358,
            });

            const fromShortLane = computeGraphTransitionEdgePath({
                bandY: 190,
                kind: 'forward',
                offset: 0,
                sourceX: 400,
                sourceY: 358,
                targetX: 700,
                targetY: 358,
            });

            // Both cross at the same height despite their endpoints differing by 138px
            expect(fromTallLane.path).toContain(',190 ');
            expect(fromShortLane.path).toContain(',190 ');
        });

        it('leaves and arrives vertically so arrowheads meet nodes from directly above', () => {
            const {path} = computeGraphTransitionEdgePath({
                bandY: 190,
                kind: 'forward',
                offset: 0,
                sourceX: 100,
                sourceY: 358,
                targetX: 400,
                targetY: 220,
            });

            // Control points share each endpoint's x, so the curve is vertical at both ends
            expect(path).toBe('M 100,358 C 100,190 400,190 400,220');
        });

        it('stacks banded arcs clear of one another by offset', () => {
            const first = computeGraphTransitionEdgePath({
                bandY: 190,
                kind: 'forward',
                offset: 0,
                sourceX: 100,
                sourceY: 358,
                targetX: 400,
                targetY: 358,
            });

            const second = computeGraphTransitionEdgePath({
                bandY: 190,
                kind: 'forward',
                offset: 1,
                sourceX: 100,
                sourceY: 358,
                targetX: 400,
                targetY: 358,
            });

            expect(second.labelY).toBeLessThan(first.labelY);
        });

        it('anchors the start label over its own lane so labels separate like lanes do', () => {
            // A long back arc and a short forward arc leaving neighbouring lanes. Their labels
            // must stay as far apart as the lanes themselves — blending the label's x toward the
            // target drags the back arc's label across the container and stacks the two.
            const longBackArc = computeGraphTransitionEdgePath({
                bandY: 190,
                kind: 'back',
                offset: 0,
                sourceX: 1483,
                sourceY: 358,
                targetX: 135,
                targetY: 358,
            });

            const shortForwardArc = computeGraphTransitionEdgePath({
                bandY: 190,
                kind: 'forward',
                offset: 0,
                sourceX: 1157,
                sourceY: 358,
                targetX: 1483,
                targetY: 358,
            });

            // Each label sits close to its own source
            expect(Math.abs(longBackArc.startLabelX - 1483)).toBeLessThan(120);
            expect(Math.abs(shortForwardArc.startLabelX - 1157)).toBeLessThan(120);

            // ...so they never overlap, unlike the two source lanes' 326px gap would suggest
            expect(Math.abs(longBackArc.startLabelX - shortForwardArc.startLabelX)).toBeGreaterThan(150);
        });

        it('ignores the band in an LR layout, where it would collapse into a vertical line', () => {
            // LR stacks lanes vertically, so both entries share roughly one x. Banding there
            // would emit `M x,y1 C x,band x,band x,y2` — a straight line drawn down through the
            // nodes rather than an arc around them.
            const {path} = computeGraphTransitionEdgePath({
                bandY: 100,
                kind: 'forward',
                offset: 0,
                sourceX: 713,
                sourceY: 400,
                targetX: 713,
                targetY: 950,
            });

            // Bows out past the frame's side instead: both control points sit right of max x,
            // so the curve has real horizontal extent rather than collapsing onto the lane
            expect(path.startsWith('M 713,400 C 749,')).toBe(true);
            expect(path.split('749,')).toHaveLength(3);
            expect(path.endsWith('713,950')).toBe(true);
        });

        it('keeps a self loop label above the lobe rather than inside it', () => {
            const {startLabelY} = computeGraphTransitionEdgePath({
                kind: 'self',
                offset: 0,
                sourceX: 400,
                sourceY: 500,
                targetX: 400,
                targetY: 500,
            });

            // The lobe spans from the node up to sourceY - 62, so the label must clear that apex
            expect(startLabelY).toBeLessThan(500 - 62);
        });

        it('keeps an LR arc label out at the bow, clear of the node text under the lane', () => {
            const {startLabelX} = computeGraphTransitionEdgePath({
                bandY: 100,
                kind: 'forward',
                offset: 0,
                sourceX: 713,
                sourceY: 400,
                targetX: 713,
                targetY: 950,
            });

            // On the lane's own x the label would land on the node's label text
            expect(startLabelX).toBeGreaterThan(713);
        });

        it('falls back to the endpoint-relative bow when no band is supplied', () => {
            const {path} = computeGraphTransitionEdgePath({
                kind: 'forward',
                offset: 0,
                sourceX: 100,
                sourceY: 358,
                targetX: 400,
                targetY: 358,
            });

            expect(path).toBe('M 100,358 C 200,322 300,322 400,358');
        });
    });

    describe('vertical spread (e.g. LR layout)', () => {
        it('bows a back edge past the max X — around the frame side — rather than above/below', () => {
            const {labelX, labelY} = computeGraphTransitionEdgePath({
                kind: 'back',
                offset: 0,
                sourceX: 300,
                sourceY: 400,
                targetX: 300,
                targetY: 0,
            });

            expect(labelX).toBeGreaterThan(300);
            expect(labelY).toBe(200);
        });
    });
});
