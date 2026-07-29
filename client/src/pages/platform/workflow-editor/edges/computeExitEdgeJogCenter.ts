// How far before the bottom bar an exit edge bends. Content always keeps at
// least a full layer gap (52px) above its frame's bottom bar, so a corner this
// close to the bar puts the horizontal leg in a strip that is empty by
// construction — the default midpoint corner put that leg mid-frame, slicing
// through sibling case content on its way to the bar.
const EXIT_EDGE_JOG_OFFSET = 16;

interface ComputeExitEdgeJogCenterProps {
    correctedSourceX: number;
    correctedSourceY: number;
    correctedTargetX: number;
    correctedTargetY: number;
    isHorizontal: boolean;
    isTriggerFanIn: boolean;
    targetNodeType?: string;
}

/**
 * Smoothstep center override for edges that terminate on a dispatcher's bottom
 * bar: the bend is pinned just before the bar so the cross-axis leg travels
 * through the structurally empty strip beside it. Short stubs (where the
 * default corner already sits in that strip) and trigger fan-in edges (which
 * pin their own bus) are left untouched.
 */
export default function computeExitEdgeJogCenter({
    correctedSourceX,
    correctedSourceY,
    correctedTargetX,
    correctedTargetY,
    isHorizontal,
    isTriggerFanIn,
    targetNodeType,
}: ComputeExitEdgeJogCenterProps): {centerX?: number; centerY?: number} {
    if (isTriggerFanIn || targetNodeType !== 'taskDispatcherBottomGhostNode') {
        return {};
    }

    if (!isHorizontal && correctedTargetY - correctedSourceY > EXIT_EDGE_JOG_OFFSET * 3) {
        return {centerY: correctedTargetY - EXIT_EDGE_JOG_OFFSET};
    }

    if (isHorizontal && correctedTargetX - correctedSourceX > EXIT_EDGE_JOG_OFFSET * 3) {
        return {centerX: correctedTargetX - EXIT_EDGE_JOG_OFFSET};
    }

    return {};
}
