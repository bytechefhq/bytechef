import {LayoutDirectionType} from '@/shared/constants';

interface ComputeBinaryCaseLabelProps {
    layoutDirection: LayoutDirectionType;
    source: string;
    sourceHandleId?: string | null;
    sourceX: number;
    targetY: number;
}

export type BinaryCaseLabelAnchorType = 'bottomCenter' | 'topCenter';

export interface BinaryCaseLabelI {
    anchor: BinaryCaseLabelAnchorType;
    text: string;
    x: number;
    y: number;
}

// Case names of the two-armed dispatchers, keyed by the top-bar id suffix.
// The bar's `-left` handle feeds the first case (TRUE / TRY) — the left arm
// in TB, the upper arm in LR — and `-right` feeds the second.
const CASE_TEXTS_BY_TOP_GHOST_SUFFIX: Record<string, [string, string]> = {
    '-condition-top-ghost': ['TRUE', 'FALSE'],
    '-onError-top-ghost': ['TRY', 'CATCH'],
};

// The arm's "+" button (24px, centered on the line midway along the 94px
// entry run) reaches 12px off the line. The label runs along the arm, so it
// clears the button by sitting this far off the line.
const BUTTON_CLEARANCE = 16;

/**
 * Labels an arm of an LR condition or on-error frame at its split bar, on the
 * arm's outer side so the pair reads mirrored around the dispatcher axis: TRUE
 * above the upper arm, FALSE below the lower arm. The label is centered on the
 * bar's line — the bar turns into the arm at that corner, so nothing is drawn
 * on the outer side. TB keeps its labels on the dispatcher node (see
 * WorkflowNode), beside the stem.
 */
export default function computeBinaryCaseLabel({
    layoutDirection,
    source,
    sourceHandleId,
    sourceX,
    targetY,
}: ComputeBinaryCaseLabelProps): BinaryCaseLabelI | undefined {
    const caseTextsEntry = Object.entries(CASE_TEXTS_BY_TOP_GHOST_SUFFIX).find(([suffix]) => source.endsWith(suffix));

    if (layoutDirection !== 'LR' || !caseTextsEntry || !sourceHandleId) {
        return undefined;
    }

    const isFirstCase = sourceHandleId === `${source}-left`;

    if (!isFirstCase && sourceHandleId !== `${source}-right`) {
        return undefined;
    }

    const [firstCaseText, secondCaseText] = caseTextsEntry[1];
    const text = isFirstCase ? firstCaseText : secondCaseText;

    return {
        anchor: isFirstCase ? 'bottomCenter' : 'topCenter',
        text,
        x: sourceX,
        y: isFirstCase ? targetY - BUTTON_CLEARANCE : targetY + BUTTON_CLEARANCE,
    };
}
