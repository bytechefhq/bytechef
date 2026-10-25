import {NODE_HEIGHT, NODE_WIDTH} from '@/shared/constants';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    CLUSTER_FRAME_CONTENT_TOP,
    CLUSTER_FRAME_HEADER_HEIGHT,
    CLUSTER_FRAME_MIN_HEIGHT,
    CLUSTER_FRAME_MIN_WIDTH,
    DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN,
    computeClusterFrameContentOrigin,
    computeClusterFrameSize,
    getClusterMemberSize,
} from './clusterFrameGeometry';

describe('computeClusterFrameSize', () => {
    it('floors a frame with no members at its minimum size', () => {
        expect(computeClusterFrameSize([])).toEqual({height: CLUSTER_FRAME_MIN_HEIGHT, width: CLUSTER_FRAME_MIN_WIDTH});
    });
});

describe('computeClusterFrameContentOrigin', () => {
    it('uses the default origin for a frame with no members', () => {
        expect(computeClusterFrameContentOrigin([], CLUSTER_FRAME_MIN_WIDTH)).toEqual(
            DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN
        );
    });

    it('starts the content a gap below the header so the root card issue badge clears the header buttons', () => {
        expect(DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN.y - CLUSTER_FRAME_HEADER_HEIGHT).toBe(16);
    });

    it('pushes the origin down so a member above the content top stays below the header', () => {
        const contentOrigin = computeClusterFrameContentOrigin(
            [{height: NODE_HEIGHT, width: NODE_WIDTH, x: 0, y: -30}],
            CLUSTER_FRAME_MIN_WIDTH
        );

        expect(contentOrigin.y).toBe(CLUSTER_FRAME_CONTENT_TOP + 30);
    });
});

describe('getClusterMemberSize', () => {
    it('falls back to the explicit node size when the member has not been measured', () => {
        const unmeasuredNode: Node = {data: {}, height: 44, id: 'unknown', position: {x: 0, y: 0}, width: 144};

        expect(getClusterMemberSize(unmeasuredNode)).toEqual({height: 44, width: 144});
    });

    it('falls back to the regular node size when the member has no size at all', () => {
        const unsizedNode: Node = {data: {}, id: 'unknown', position: {x: 0, y: 0}};

        expect(getClusterMemberSize(unsizedNode)).toEqual({height: NODE_HEIGHT, width: NODE_WIDTH});
    });
});
