import Badge from '@/components/Badge/Badge';
import {
    GRAPH_TRANSITION_EDGE_COLOR,
    GRAPH_TRANSITION_EDGE_DANGLING_COLOR,
    GRAPH_TRANSITION_EDGE_SELECTED_COLOR,
} from '@/shared/constants';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, Position, getBezierPath} from '@xyflow/react';
import {useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useExecutedEdgeStatus from './useExecutedEdgeStatus';

const BACKWARD_BOW_MIN = 48;
const BACKWARD_BOW_MAX = 220;
const BACKWARD_BOW_RATIO = 0.35;

const SELF_BOW = 74;

const BOW_RANKS = 4;
const BOW_RANK_STEP = 26;

export interface GraphTransitionEdgeDataI {
    condition?: string;
    dangling?: boolean;
    dynamic?: boolean;
    graphId: string;
    index: number;
    readOnly?: boolean;
    to: string;
}

export default function GraphTransitionEdge({
    data,
    id,
    markerEnd,
    selected,
    source,
    sourcePosition,
    sourceX,
    sourceY,
    target,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const [isHovered, setIsHovered] = useState(false);

    const setGraphTransitionLabelPosition = useWorkflowEditorStore((state) => state.setGraphTransitionLabelPosition);

    const {condition, dangling, dynamic, graphId, index, to} = (data ?? {}) as Partial<GraphTransitionEdgeDataI>;

    const executedGraphTransition =
        graphId !== undefined && to !== undefined ? {dynamic, from: source, graphId, to} : undefined;

    const executedEdgeStatus = useExecutedEdgeStatus(id, executedGraphTransition);

    const returnsToSource = source === target;
    const isHorizontalFlow = sourcePosition === Position.Right;

    const {labelX, labelY, path} = useMemo(() => {
        const flowsBackwards = returnsToSource || (isHorizontalFlow ? targetX < sourceX : targetY < sourceY);

        if (flowsBackwards) {
            const signedSpan = isHorizontalFlow ? sourceX - targetX : sourceY - targetY;
            const span = Math.abs(signedSpan);
            const rankReach = ((index ?? 0) % BOW_RANKS) * BOW_RANK_STEP;
            const spanBow = BACKWARD_BOW_MIN + span * BACKWARD_BOW_RATIO;

            const bow = returnsToSource ? SELF_BOW + rankReach : Math.min(BACKWARD_BOW_MAX, spanBow) + rankReach;

            const path = isHorizontalFlow
                ? `M ${sourceX},${sourceY} C ${sourceX + bow},${sourceY - bow} ${targetX - bow},${targetY - bow} ${targetX},${targetY}`
                : `M ${sourceX},${sourceY} C ${sourceX - bow},${sourceY + bow} ${targetX - bow},${targetY - bow} ${targetX},${targetY}`;

            return {
                labelX: isHorizontalFlow ? (sourceX + targetX) / 2 : Math.min(sourceX, targetX) - bow / 2,
                labelY: isHorizontalFlow ? Math.min(sourceY, targetY) - bow / 2 : (sourceY + targetY) / 2,
                path,
            };
        }

        const [bezierPath, bezierLabelX, bezierLabelY] = getBezierPath({
            sourcePosition,
            sourceX,
            sourceY,
            targetPosition,
            targetX,
            targetY,
        });

        return {labelX: bezierLabelX, labelY: bezierLabelY, path: bezierPath};
    }, [index, isHorizontalFlow, returnsToSource, sourcePosition, sourceX, sourceY, targetPosition, targetX, targetY]);

    const isActive = isHovered || !!selected;

    const strokeColor = dangling
        ? GRAPH_TRANSITION_EDGE_DANGLING_COLOR
        : isActive
          ? GRAPH_TRANSITION_EDGE_SELECTED_COLOR
          : GRAPH_TRANSITION_EDGE_COLOR;

    let label: string | undefined;

    if (dynamic) {
        label = isActive ? `dynamic: ${to ?? ''}` : 'dynamic';
    } else {
        label = condition;
    }

    useEffect(() => {
        setGraphTransitionLabelPosition(id, {labelX, labelY});
    }, [id, labelX, labelY, setGraphTransitionLabelPosition]);

    return (
        <>
            <path
                d={path}
                fill="none"
                onMouseEnter={() => setIsHovered(true)}
                onMouseLeave={() => setIsHovered(false)}
                stroke="transparent"
                strokeWidth={20}
            />

            <BaseEdge
                className={twMerge(
                    executedEdgeStatus === 'COMPLETED' && 'stroke-green-500',
                    executedEdgeStatus === 'FAILED' && 'stroke-red-500'
                )}
                id={id}
                markerEnd={markerEnd}
                path={path}
                style={{
                    fill: 'none',
                    ...(executedEdgeStatus ? {} : {stroke: strokeColor}),
                    strokeDasharray: dynamic ? '6 4' : undefined,
                    strokeWidth: isActive ? 2.5 : 2,
                }}
            />

            {label && (
                <EdgeLabelRenderer>
                    <div
                        className="nodrag nopan pointer-events-none absolute z-10"
                        style={{transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`}}
                    >
                        <Badge
                            styleType={
                                dangling ? 'warning-filled' : isActive ? 'secondary-filled' : 'secondary-outline'
                            }
                        >
                            <span className={twMerge(!isActive && 'max-w-64 truncate')}>{label}</span>
                        </Badge>
                    </div>
                </EdgeLabelRenderer>
            )}
        </>
    );
}
