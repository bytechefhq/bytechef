import {LayoutDirectionType} from '@/shared/constants';

interface ComputeBranchCaseLabelPositionProps {
    hasEdgeButton?: boolean;
    layoutDirection: LayoutDirectionType;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

const EDGE_BUTTON_OFFSET = 10;

export default function computeBranchCaseLabelPosition({
    hasEdgeButton,
    layoutDirection,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: ComputeBranchCaseLabelPositionProps): {x: number; y: number} {
    if (layoutDirection === 'LR') {
        return {
            x: sourceX,
            y: targetY + (hasEdgeButton ? EDGE_BUTTON_OFFSET : 0),
        };
    }

    return {
        x: targetX,
        y: sourceY,
    };
}
