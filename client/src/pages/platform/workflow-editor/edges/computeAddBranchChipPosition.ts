import {LayoutDirectionType} from '@/shared/constants';

interface ComputeAddBranchChipPositionProps {
    layoutDirection: LayoutDirectionType;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

const CORNER_CLEARANCE = 36;

export default function computeAddBranchChipPosition({
    layoutDirection,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: ComputeAddBranchChipPositionProps): {x: number; y: number} {
    if (layoutDirection === 'LR') {
        return {x: sourceX, y: Math.max((sourceY + targetY) / 2, targetY - CORNER_CLEARANCE)};
    }

    return {x: Math.max((sourceX + targetX) / 2, targetX - CORNER_CLEARANCE), y: sourceY};
}
