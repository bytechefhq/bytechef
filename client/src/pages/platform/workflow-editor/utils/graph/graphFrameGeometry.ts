import {LR_NODE_LABEL_BLOCK_HEIGHT, LayoutDirectionType} from '@/shared/constants';
import {GraphTransitionType} from '@/shared/types';

import {loadElk} from '../elkLayoutUtils';
import {isDynamicTransitionTarget} from './graphTransitionMutations';

import type {ElkExtendedEdge, ElkNode} from 'elkjs/lib/elk-api';

export const GRAPH_FRAME_PADDING = 24;
export const GRAPH_FRAME_HEADER_HEIGHT = 40;
export const GRAPH_FRAME_MIN_WIDTH = 320;
export const GRAPH_FRAME_MIN_HEIGHT = 200;
export const GRAPH_START_SIZE = {height: 32, width: 72};
export const GRAPH_MEMBER_NOMINAL_SIZE = {height: 100, width: 240};

export const GRAPH_MEMBER_BOX_WIDTH = 72;

export const GRAPH_MEMBER_SPACING = 40;

export const GRAPH_LABEL_CLEARANCE = 72;

export const GRAPH_LOOP_GUTTER = 72;

export const GRAPH_FRAME_ID_ATTRIBUTE = 'data-graph-frame-id';

export const GRAPH_FRAME_ID_SUFFIX = '-graph-frame';

export const GRAPH_MEMBER_LAYER_SPACING = 80;

export function getGraphFrameId(graphId: string): string {
    return `${graphId}${GRAPH_FRAME_ID_SUFFIX}`;
}

export function getGraphIdFromFrameNodeId(nodeId: string | undefined): string | undefined {
    if (!nodeId || !nodeId.endsWith(GRAPH_FRAME_ID_SUFFIX)) {
        return undefined;
    }

    return nodeId.slice(0, -GRAPH_FRAME_ID_SUFFIX.length);
}

export interface GraphMemberBoxI {
    height: number;
    name: string;
    paintedWidth?: number;
    width: number;
    x: number;
    y: number;
}

export function getGraphMemberPaintedWidth(memberBox: GraphMemberBoxI): number {
    return memberBox.paintedWidth ?? memberBox.width;
}

const GRAPH_START_PINNED_BOX_NAME = '<graph-start>';

export function getGraphStartPinnedBox(): GraphMemberBoxI {
    return {
        height: GRAPH_START_SIZE.height,
        name: GRAPH_START_PINNED_BOX_NAME,
        width: GRAPH_START_SIZE.width,
        x: GRAPH_FRAME_PADDING,
        y: 0,
    };
}

export function toFrameChildPosition(position: {x: number; y: number}): {x: number; y: number} {
    return {x: position.x, y: position.y + GRAPH_FRAME_HEADER_HEIGHT};
}

export function fromFrameChildPosition(position: {x: number; y: number}): {x: number; y: number} {
    return {x: position.x, y: position.y - GRAPH_FRAME_HEADER_HEIGHT};
}

export function computeGraphFrameSize(
    memberBoxes: GraphMemberBoxI[],
    direction: LayoutDirectionType = 'TB'
): {height: number; width: number} {
    if (memberBoxes.length === 0) {
        return {height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH};
    }

    const leftEdges = memberBoxes.map((memberBox) => memberBox.x);
    const paintedRightEdges = memberBoxes.map((memberBox) => memberBox.x + getGraphMemberPaintedWidth(memberBox));
    const topEdges = memberBoxes.map((memberBox) => memberBox.y);
    const bottomEdges = memberBoxes.map((memberBox) => memberBox.y + memberBox.height);

    const paintedLeft = Math.min(...leftEdges);
    const paintedRight = Math.max(...paintedRightEdges);
    const paintedTop = Math.min(...topEdges);
    const maxBottom = Math.max(...bottomEdges);

    const bottomInset =
        direction === 'LR' ? Math.max(paintedTop, GRAPH_LOOP_GUTTER) + GRAPH_FRAME_HEADER_HEIGHT : GRAPH_FRAME_PADDING;

    const contentHeight = GRAPH_FRAME_HEADER_HEIGHT + maxBottom + bottomInset;
    const contentWidth = paintedRight + Math.max(paintedLeft, GRAPH_LOOP_GUTTER);

    return {
        height: Math.max(GRAPH_FRAME_MIN_HEIGHT, contentHeight),
        width: Math.max(GRAPH_FRAME_MIN_WIDTH, contentWidth),
    };
}

export function separateOverlappingGraphMembers(
    memberBoxes: GraphMemberBoxI[],
    direction: LayoutDirectionType
): GraphMemberBoxI[] {
    const isHorizontalFlow = direction === 'LR';

    const settledBoxes = memberBoxes.map((memberBox) => ({...memberBox}));

    const mainStart = (memberBox: GraphMemberBoxI) => (isHorizontalFlow ? memberBox.x : memberBox.y);
    const mainSize = (memberBox: GraphMemberBoxI) =>
        isHorizontalFlow ? getGraphMemberPaintedWidth(memberBox) : memberBox.height;
    const crossStart = (memberBox: GraphMemberBoxI) => (isHorizontalFlow ? memberBox.y : memberBox.x);
    const crossSize = (memberBox: GraphMemberBoxI) =>
        isHorizontalFlow ? memberBox.height : getGraphMemberPaintedWidth(memberBox);

    const flowOrder = [...settledBoxes].sort(
        (firstBox, secondBox) =>
            mainStart(firstBox) - mainStart(secondBox) || crossStart(firstBox) - crossStart(secondBox)
    );

    for (let boxIndex = 1; boxIndex < flowOrder.length; boxIndex++) {
        const memberBox = flowOrder[boxIndex];

        for (let settledIndex = 0; settledIndex < boxIndex; settledIndex++) {
            const settledBox = flowOrder[settledIndex];

            const overlapsAcrossFlow =
                crossStart(memberBox) < crossStart(settledBox) + crossSize(settledBox) &&
                crossStart(settledBox) < crossStart(memberBox) + crossSize(memberBox);
            const overlapsAlongFlow =
                mainStart(memberBox) < mainStart(settledBox) + mainSize(settledBox) &&
                mainStart(settledBox) < mainStart(memberBox) + mainSize(memberBox);

            if (!overlapsAcrossFlow || !overlapsAlongFlow) {
                continue;
            }

            const clearedStart = mainStart(settledBox) + mainSize(settledBox) + GRAPH_MEMBER_SPACING;

            if (isHorizontalFlow) {
                memberBox.x = clearedStart;
            } else {
                memberBox.y = clearedStart;
            }
        }
    }

    return settledBoxes;
}

export function findFreeSpot(
    memberBoxes: GraphMemberBoxI[],
    direction: LayoutDirectionType = 'TB'
): {x: number; y: number} | undefined {
    if (memberBoxes.length === 0) {
        return undefined;
    }

    const isHorizontalFlow = direction === 'LR';

    const flowEndOf = (memberBox: GraphMemberBoxI) =>
        isHorizontalFlow ? memberBox.x + getGraphMemberPaintedWidth(memberBox) : memberBox.y + memberBox.height;
    const crossStartOf = (memberBox: GraphMemberBoxI) => (isHorizontalFlow ? memberBox.y : memberBox.x);

    const lastBox = memberBoxes.reduce(
        (last, memberBox) =>
            flowEndOf(memberBox) > flowEndOf(last) ||
            (flowEndOf(memberBox) === flowEndOf(last) && crossStartOf(memberBox) < crossStartOf(last))
                ? memberBox
                : last,
        memberBoxes[0]
    );

    const nextLayerStart = flowEndOf(lastBox) + GRAPH_MEMBER_LAYER_SPACING;

    return isHorizontalFlow ? {x: nextLayerStart, y: lastBox.y} : {x: lastBox.x, y: nextLayerStart};
}

type GraphMemberSizeType = {height: number; labelOverhang?: number; name: string; width: number};

interface AutoPlaceGraphMembersProps {
    direction?: LayoutDirectionType;
    memberSizes: GraphMemberSizeType[];
    pinnedBoxes: GraphMemberBoxI[];
    startNodeName?: string;
    transitions: GraphTransitionType[];
}

export async function autoPlaceGraphMembers({
    direction = 'TB',
    memberSizes,
    pinnedBoxes,
    startNodeName,
    transitions,
}: AutoPlaceGraphMembersProps): Promise<Record<string, {x: number; y: number}>> {
    const isHorizontalFlow = direction === 'LR';
    const memberNameList = memberSizes.map((memberSize) => memberSize.name);
    const memberNames = new Set(memberNameList);

    const staticTransitions = transitions.filter(
        (transition) =>
            memberNames.has(transition.from) &&
            !isDynamicTransitionTarget(transition.to) &&
            memberNames.has(transition.to)
    );

    const staticEdges: ElkExtendedEdge[] = staticTransitions.map((transition, transitionIndex) => ({
        id: `${transition.from}->${transition.to}-${transitionIndex}`,
        sources: [transition.from],
        targets: [transition.to],
    }));

    const staticallyEnteredNames = new Set(staticTransitions.map((transition) => transition.to));

    const dynamicSourceNames = new Set(
        transitions
            .filter((transition) => memberNames.has(transition.from) && isDynamicTransitionTarget(transition.to))
            .map((transition) => transition.from)
    );

    const dynamicTargetCandidateNames = memberNameList.filter(
        (memberName) => memberName !== startNodeName && !staticallyEnteredNames.has(memberName)
    );

    const dynamicHintEdges: ElkExtendedEdge[] = [...dynamicSourceNames].flatMap((sourceName) =>
        dynamicTargetCandidateNames
            .filter((candidateName) => candidateName !== sourceName)
            .map((candidateName) => ({
                id: `${sourceName}~>${candidateName}`,
                sources: [sourceName],
                targets: [candidateName],
            }))
    );

    const elkEdges = [...staticEdges, ...dynamicHintEdges];

    const labelOverhangs = memberSizes.map((memberSize) => memberSize.labelOverhang ?? 0);

    const crossAxisLabelRoom = isHorizontalFlow ? LR_NODE_LABEL_BLOCK_HEIGHT : Math.max(0, ...labelOverhangs);

    const crossAxisGap = `${GRAPH_MEMBER_SPACING + crossAxisLabelRoom}`;

    const elkGraph: ElkNode = {
        children: memberSizes.map((memberSize) => ({
            height: memberSize.height,
            id: memberSize.name,
            ...(memberSize.name === startNodeName
                ? {layoutOptions: {'elk.layered.layering.layerConstraint': 'FIRST'}}
                : {}),
            width: memberSize.width,
        })),
        edges: elkEdges,
        id: 'graph',
        layoutOptions: {
            'elk.algorithm': 'layered',
            'elk.direction': isHorizontalFlow ? 'RIGHT' : 'DOWN',
            'elk.layered.spacing.nodeNodeBetweenLayers': `${GRAPH_MEMBER_LAYER_SPACING}`,
            'elk.spacing.componentComponent': crossAxisGap,
            'elk.spacing.nodeNode': crossAxisGap,
        },
    };

    const elk = await loadElk();
    const layoutedGraph = await elk.layout(elkGraph);

    const pinnedFlowEnds = pinnedBoxes.map((memberBox) =>
        isHorizontalFlow ? memberBox.x + memberBox.width : memberBox.y + memberBox.height
    );

    const pinnedEnd = pinnedBoxes.length === 0 ? 0 : Math.max(...pinnedFlowEnds);
    const flowOffset = pinnedEnd + GRAPH_FRAME_PADDING;

    const laidOutChildren = layoutedGraph.children ?? [];

    const crossStartOf = (child: ElkNode) => (isHorizontalFlow ? (child.y ?? 0) : (child.x ?? 0));
    const crossEndOf = (child: ElkNode) =>
        crossStartOf(child) + (isHorizontalFlow ? (child.height ?? 0) : (child.width ?? 0));

    const crossStarts = laidOutChildren.map(crossStartOf);
    const crossEnds = laidOutChildren.map(crossEndOf);

    const blockStart = laidOutChildren.length === 0 ? 0 : Math.min(...crossStarts);
    const blockEnd = laidOutChildren.length === 0 ? 0 : Math.max(...crossEnds);

    const crossInset = getCrossAxisInset({blockEnd, blockStart, isHorizontalFlow, laidOutChildren, memberSizes});

    const positionsByName: Record<string, {x: number; y: number}> = {};

    for (const layoutedChild of laidOutChildren) {
        const crossPosition = crossStartOf(layoutedChild) - blockStart + crossInset;

        positionsByName[layoutedChild.id] = {
            x: isHorizontalFlow ? (layoutedChild.x ?? 0) + flowOffset : crossPosition,
            y: isHorizontalFlow ? crossPosition : (layoutedChild.y ?? 0) + flowOffset,
        };
    }

    return positionsByName;
}

interface GetCrossAxisInsetProps {
    blockEnd: number;
    blockStart: number;
    isHorizontalFlow: boolean;
    laidOutChildren: ElkNode[];
    memberSizes: GraphMemberSizeType[];
}

function getCrossAxisInset({
    blockEnd,
    blockStart,
    isHorizontalFlow,
    laidOutChildren,
    memberSizes,
}: GetCrossAxisInsetProps): number {
    if (isHorizontalFlow || laidOutChildren.length === 0) {
        return GRAPH_LOOP_GUTTER;
    }

    const labelOverhangEntries = memberSizes.map((memberSize): [string, number] => [
        memberSize.name,
        memberSize.labelOverhang ?? 0,
    ]);

    const labelOverhangByName = new Map(labelOverhangEntries);

    const labelEnds = laidOutChildren.map(
        (child) => (child.x ?? 0) + (child.width ?? 0) + (labelOverhangByName.get(child.id) ?? 0)
    );

    const labelEnd = Math.max(...labelEnds);

    const unclampedLabelOverflow = labelEnd - blockEnd;

    const labelOverflow = Math.max(0, unclampedLabelOverflow);

    const labelClearanceInset = labelOverflow + GRAPH_LABEL_CLEARANCE;

    const minimumWidthInset = (GRAPH_FRAME_MIN_WIDTH - (blockEnd - blockStart)) / 2;

    return Math.max(GRAPH_LOOP_GUTTER, labelClearanceInset, minimumWidthInset);
}
