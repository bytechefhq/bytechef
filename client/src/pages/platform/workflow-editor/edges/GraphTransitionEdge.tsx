import Badge from '@/components/Badge/Badge';
import {GRAPH_TRANSITION_EDGE_COLOR, GRAPH_TRANSITION_EDGE_SELECTED_COLOR} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, useStore} from '@xyflow/react';
import {useState} from 'react';

import computeGraphTransitionEdgePath, {GraphTransitionEdgeKindType} from './computeGraphTransitionEdgePath';

export interface GraphTransitionEdgeDataI {
    graphId: string;
    kind: GraphTransitionEdgeKindType;
    offset: number;
    sourceIndex: number;
    targetIndex: number;
    targetName: string;
}

// Clearance between the topmost lane entry and the band the arcs travel in
const GRAPH_TRANSITION_BAND_GAP = 30;

/**
 * Renders one derived `next`-expression transition (`deriveGraphTransitionEdges`) as an
 * OVERLAY edge on top of the stable lane layout — per the phase-3 plan's architecture
 * decision, transitions never participate in layout, they are purely a paint-time affordance.
 *
 * Visually distinct from the structural `workflow`/`smoothstep` lane-chain edges on purpose:
 * a curved (never orthogonal) path, an arrowhead marker, and a muted color that is neither the
 * neutral structural-edge gray nor the brand color used for active/selected canvas chrome.
 * Path geometry (curve shape, self-loop, back-edge side-routing) is delegated to
 * `computeGraphTransitionEdgePath`, kept pure and unit-tested on its own.
 *
 * The target-name label chip only renders on hover or when the edge is selected — the same
 * "reveal on interaction" affordance as `WorkflowEdge`'s add-node button, so the overlay does
 * not visually compete with the lane's own transition badges (`GraphTransitionBadges`) at rest.
 */
export default function GraphTransitionEdge({
    data,
    id,
    markerEnd,
    selected,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: EdgeProps) {
    const [isHovered, setIsHovered] = useState(false);

    const {graphId, kind, offset, targetName} = (data ?? {}) as Partial<GraphTransitionEdgeDataI>;

    // Every arc in one graph must route through the SAME band, but the band only exists after
    // layout — so it is derived here from the graph's own lane entry points rather than baked in
    // at edge-construction time. Selecting a single number keeps this from re-rendering on
    // unrelated node changes.
    const bandY = useStore((state) => {
        if (!graphId) {
            return undefined;
        }

        let highestEntryY = Number.POSITIVE_INFINITY;

        state.nodeLookup.forEach((node) => {
            const nodeData = node.data as NodeDataType;

            const isLaneChainEntry = nodeData?.graphData?.graphId === graphId && nodeData?.graphData?.index === 0;
            const isLanePlaceholder = nodeData?.graphId === graphId && node.type === 'placeholder';

            if (isLaneChainEntry || isLanePlaceholder) {
                highestEntryY = Math.min(highestEntryY, node.position.y);
            }
        });

        return Number.isFinite(highestEntryY) ? highestEntryY - GRAPH_TRANSITION_BAND_GAP : undefined;
    });

    const {path, startLabelX, startLabelY} = computeGraphTransitionEdgePath({
        bandY,
        kind: kind ?? 'forward',
        offset: offset ?? 0,
        sourceX,
        sourceY,
        targetX,
        targetY,
    });

    const isActive = isHovered || !!selected;
    const strokeColor = isActive ? GRAPH_TRANSITION_EDGE_SELECTED_COLOR : GRAPH_TRANSITION_EDGE_COLOR;

    return (
        <>
            {/* Wide, invisible hit area — the visible stroke below is intentionally thin, so
                this widens the hover target without affecting the painted curve. */}
            <path
                d={path}
                fill="none"
                onMouseEnter={() => setIsHovered(true)}
                onMouseLeave={() => setIsHovered(false)}
                stroke="transparent"
                strokeWidth={20}
            />

            <BaseEdge
                id={id}
                markerEnd={markerEnd}
                path={path}
                style={{
                    fill: 'none',
                    stroke: strokeColor,
                    strokeDasharray: '6 4',
                    strokeWidth: isActive ? 2.5 : 2,
                }}
            />

            {targetName && (
                <EdgeLabelRenderer>
                    <div
                        className="nodrag nopan pointer-events-none absolute z-10"
                        style={{
                            transform: `translate(-50%, -50%) translate(${startLabelX}px, ${startLabelY}px)`,
                        }}
                    >
                        <Badge label={targetName} styleType={isActive ? 'secondary-filled' : 'secondary-outline'} />
                    </div>
                </EdgeLabelRenderer>
            )}
        </>
    );
}
