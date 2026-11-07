import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, LayoutDirectionType} from '@/shared/constants';

interface ComputeBinaryCaseLabelProps {
    layoutDirection: LayoutDirectionType;
    source: string;
    sourceHandleId?: string | null;
    sourceX: number;
    targetY: number;
}

export type BinaryCaseLabelAnchorType = 'bottomCenter' | 'topCenter';

export interface BinaryCaseLabelConditionCaseI {
    caseName: string;
    conditionName: string;
}

export interface BinaryCaseLabelI {
    anchor: BinaryCaseLabelAnchorType;
    conditionCase?: BinaryCaseLabelConditionCaseI;
    text: string;
    x: number;
    y: number;
}

// Case names of the two-armed dispatchers, keyed by the top-bar id suffix.
// The bar's `-left` handle feeds the first case (TRUE / TRY) — the left arm
// in TB, the upper arm in LR — and `-right` feeds the second.
const CONDITION_TOP_GHOST_SUFFIX = '-condition-top-ghost';

const CASE_TEXTS_BY_TOP_GHOST_SUFFIX: Record<string, [string, string]> = {
    '-onError-top-ghost': ['TRY', 'CATCH'],
    [CONDITION_TOP_GHOST_SUFFIX]: ['TRUE', 'FALSE'],
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

    const [topGhostSuffix, [firstCaseText, secondCaseText]] = caseTextsEntry;
    const text = isFirstCase ? firstCaseText : secondCaseText;

    const conditionCase =
        topGhostSuffix === CONDITION_TOP_GHOST_SUFFIX
            ? {
                  caseName: isFirstCase ? CONDITION_CASE_TRUE : CONDITION_CASE_FALSE,
                  conditionName: source.slice(0, -CONDITION_TOP_GHOST_SUFFIX.length),
              }
            : undefined;

    return {
        anchor: isFirstCase ? 'bottomCenter' : 'topCenter',
        ...(conditionCase && {conditionCase}),
        text,
        x: sourceX,
        y: isFirstCase ? targetY - BUTTON_CLEARANCE : targetY + BUTTON_CLEARANCE,
    };
}
