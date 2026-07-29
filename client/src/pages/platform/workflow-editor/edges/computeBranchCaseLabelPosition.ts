import {LayoutDirectionType} from '@/shared/constants';

interface ComputeBranchCaseLabelPositionProps {
    layoutDirection: LayoutDirectionType;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

export type BranchCaseLabelAnchorType = 'above' | 'below' | 'center';

const EDGE_BUTTON_OFFSET = 10;

// In LR the chip hangs ABOVE its row's entry line (the line stays fully
// visible), right-anchored just past the split bar so it labels the row start.
// 44 = the 94px entry run minus the row's add-button, which sits on the line
// from bar + 46 — the chip's right edge stops before the button's column.
const LR_ROW_LINE_OVERHANG = 44;

// Gap between the chip's edge and the row line it labels.
const LR_LINE_GAP = 12;

// With an odd case count the middle row shares the dispatcher's axis, so the
// space above its line holds the dispatcher icon (36px above the axis) —
// that row's chip bottom clears the icon band with breathing room. The lift
// also absorbs the bar's source-handle offset (up to 7px below the axis).
const LR_AXIS_ROW_LIFT = 56;

const LR_AXIS_ROW_TOLERANCE = 40;

// The dispatcher's name/label text hangs below its icon to roughly this far
// past the axis. The chip of the row just BELOW the axis would run through
// that text if placed above its line, so it drops below the line instead.
const LR_LABEL_BAND = 160;

export default function computeBranchCaseLabelPosition({
    layoutDirection,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: ComputeBranchCaseLabelPositionProps): {anchor: BranchCaseLabelAnchorType; x: number; y: number} {
    if (layoutDirection === 'LR') {
        const rowOffset = targetY - sourceY;
        const isDispatcherAxisRow = Math.abs(rowOffset) < LR_AXIS_ROW_TOLERANCE;
        const isRowBelowDispatcherLabel = !isDispatcherAxisRow && rowOffset > 0 && rowOffset < LR_LABEL_BAND;

        if (isRowBelowDispatcherLabel) {
            return {
                anchor: 'below',
                x: sourceX + LR_ROW_LINE_OVERHANG,
                y: targetY + LR_LINE_GAP,
            };
        }

        return {
            anchor: 'above',
            x: sourceX + LR_ROW_LINE_OVERHANG,
            y: isDispatcherAxisRow ? sourceY - LR_AXIS_ROW_LIFT : targetY - LR_LINE_GAP,
        };
    }

    return {
        anchor: 'center',
        x: targetX,
        y: sourceY + EDGE_BUTTON_OFFSET,
    };
}
