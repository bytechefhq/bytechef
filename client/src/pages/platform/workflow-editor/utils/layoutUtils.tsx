import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    CLUSTER_ROOT_NODE_WIDTH,
    CONDITION_CASE_FALSE,
    CONDITION_CASE_TRUE,
    DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    GRAPH_START_EDGE_TYPE,
    GRAPH_TRANSITION_EDGE_TYPE,
    LayoutDirectionType,
    NODE_HEIGHT,
    NODE_WIDTH,
    ON_ERROR_ERROR_BRANCH,
    ON_ERROR_MAIN_BRANCH,
    ON_ERROR_WIRE_KEY_ERROR_BRANCH,
    ON_ERROR_WIRE_KEY_MAIN_BRANCH,
    PLACEHOLDER_NODE_HEIGHT,
    ROOT_CLUSTER_WIDTH,
    TASK_DISPATCHER_NAMES,
    TRIGGER_PLACEHOLDER_NODE_ID,
    TRIGGER_PLACEHOLDER_NODE_SIZE,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    WorkflowTask,
    WorkflowTrigger,
} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    BranchChildTasksType,
    ConditionChildTasksType,
    EachChildTasksType,
    ForkJoinChildTasksType,
    GraphChildTasksType,
    LoopChildTasksType,
    MapChildTasksType,
    NodeDataType,
    OnErrorChildTasksType,
    ParallelChildTasksType,
} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon, PlayIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {calculateNodeWidth, getHandlePosition} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {getConditionBranchSide} from './createConditionEdges';
import {getForkJoinBranchSide} from './createForkJoinEdges';
import {getOnErrorBranchSide} from './createOnErrorEdges';
import {getCrossAxis, getCrossAxisNodeSize} from './directionUtils';
import {
    adjustBottomGhostForMovedChildren,
    alignBranchCaseChildren,
    alignChainNodesCrossAxis,
    alignDispatcherGhostsCrossAxis,
    alignSimpleConditionChildren,
    alignTrailingPlaceholder,
    applySavedPositions,
    centerDispatcherChildrenOnMainAxis,
    centerDispatcherPlaceholdersOnMainAxis,
    centerLRSmallNodes,
    centerNodesAfterBottomGhost,
    constrainBranchGhostsCrossAxis,
    constrainConditionGhostsCrossAxis,
    constrainLeftGhostPositions,
    constrainOnErrorGhostsCrossAxis,
    containsNodePosition,
    hasConfiguredClusterElements,
    positionConditionCasePlaceholders,
    positionOnErrorCasePlaceholders,
    pullSimpleOnErrorChildrenInward,
    separateOverlappingConditionChildren,
    separateOverlappingOnErrorChildren,
    shiftConditionBranchContent,
    shiftOnErrorBranchContent,
} from './postDagreConstraints';
import {TASK_DISPATCHER_CONFIG, getParentTaskDispatcherTask} from './taskDispatcherConfig';

export const CLUSTER_ELEMENT_GAP = 70;
export const CLUSTER_ELEMENT_LABEL_PADDING = 20;
export const CLUSTER_ELEMENT_OVERLAP_PADDING = 20;
export const CLUSTER_ROOT_GAP = 40;
const TRIGGER_PLACEHOLDER_GAP = 40;

// Triggers render as a ~72px icon box with the label overflowing to its right.
// Reserving the full NODE_WIDTH (sized for tasks) spaces the trigger row too far
// apart, so triggers get a tighter dagre footprint in the vertical (TB) layout.
const TRIGGER_NODE_DAGRE_WIDTH = 160;

// The visible trigger box is a 72px icon square (w-18/min-h-18). Used to vertically
// center the small add-trigger "+" slot on that square.
const TRIGGER_NODE_BOX_SIZE = 72;
// Half the 72px icon box every node carries its chain handles at the centre of. dagre positions
// are top-left of that box, so the chain axis is this far past a node's cross-axis position —
// the same half-box `triggerCrossHalf` uses when centring the canvas.
const NODE_ANCHOR_HALF = 36;
// The add-trigger slot's visible box is inset from its node by this horizontal margin
const TRIGGER_PLACEHOLDER_BOX_MARGIN = 8;
// A trigger's three RENDERED label lines (title, operation, name). Distinct from the layout
// footprint TRIGGER_NODE_DAGRE_WIDTH reserves, which is larger — placing against the reservation
// leaves visibly empty space below the label in LR, where the label stacks under the icon.
const TRIGGER_LABEL_BLOCK_HEIGHT = 64;

// In TB a node's title/description block renders to the RIGHT of its 72px icon, reaching up to
// ~236px past the icon's center (measured 272px DOM on long labels) — while the dagre footprint
// models the node as 240px CENTERED on the icon (±120). The overhang is estimated PER NODE from
// its label texts so short-labelled nodes don't pay the worst case: the title truncates at
// max-w-48 (192px) + the 8px icon margin, which caps the estimate at 200.
const NODE_LABEL_MAX_CROSS_OVERHANG = 200;

// Generous per-character upper bound for the 14px label text (semibold title, monospace operation
// name), plus the icon→label margin.
const LABEL_CHAR_WIDTH = 9;
const LABEL_BLOCK_MARGIN = 16;

/**
 * How far a node's label block reaches past its icon in TB, estimated from the label texts
 * themselves. Deliberately an estimate rather than a measurement: nodes rebuilt from the workflow
 * definition carry no ReactFlow `measured` size, so anything keyed off one silently never runs.
 */
export function getLabelCrossOverhang(node: Node): number {
    const nodeData = node.data as NodeDataType;

    const longestLabelLength = Math.max(
        String(nodeData.title || nodeData.label || '').length,
        String(nodeData.operationName || '').length,
        String(nodeData.workflowNodeName || nodeData.name || '').length
    );

    return Math.min(NODE_LABEL_MAX_CROSS_OVERHANG, LABEL_BLOCK_MARGIN + longestLabelLength * LABEL_CHAR_WIDTH);
}

let dagre: typeof import('@dagrejs/dagre') | null = null;

const loadDagre = async () => {
    if (!dagre) {
        dagre = await import('@dagrejs/dagre');
    }

    return dagre;
};

// A graph's chain runs dispatcher -> top ghost -> frame -> bottom ghost like every other task
// dispatcher's, but its ghosts are bare anchors: no branch rails hang off them and they paint no
// bar. The rank each of those edges is widened by elsewhere buys room for chrome a graph does not
// have, so they are taken back to a single rank.
const GRAPH_TOP_GHOST_SUFFIX = '-graph-top-ghost';
const GRAPH_BOTTOM_GHOST_SUFFIX = '-graph-bottom-ghost';

export const calculateNodeHeight = (node: Node) => {
    const isTopGhostNode = node.type === 'taskDispatcherTopGhostNode';
    const isBottomGhostNode = node.type === 'taskDispatcherBottomGhostNode';
    const isLeftGhostNode = node.type === 'taskDispatcherLeftGhostNode';
    const isPlaceholderNode = node.type === 'placeholder';
    const isGhostNode = isTopGhostNode || isBottomGhostNode || isLeftGhostNode;

    let height = NODE_HEIGHT;

    if (isPlaceholderNode || isGhostNode) {
        height = PLACEHOLDER_NODE_HEIGHT;

        if (isTopGhostNode || isBottomGhostNode) {
            height = 0;
        }
    }

    return height;
};

/**
 * Returns the approximate rendered main-axis size (width in LR mode) for a node.
 * Dagre reports center coordinates; subtracting half this value converts to the
 * top-left position that ReactFlow expects.
 */
function getRenderedMainAxisSize(node: Node, direction: LayoutDirectionType): number {
    // A graph frame paints its whole box, so — unlike every other node type, whose rendered box is
    // placed with its top-left ON the footprint centre — its box has to start at the footprint's
    // leading edge or it would overrun the next rank by half its size.
    const graphFrame = (node.data as NodeDataType)?.graphFrame;

    if (graphFrame) {
        return direction === 'LR' ? graphFrame.width : graphFrame.height;
    }

    if (direction !== 'LR') {
        return 0;
    }

    if (node.type === 'taskDispatcherLeftGhostNode') {
        return 16;
    }

    if (node.type === 'taskDispatcherTopGhostNode' || node.type === 'taskDispatcherBottomGhostNode') {
        return 2;
    }

    if (node.type === 'clusterRoot') {
        return hasConfiguredClusterElements(node) ? 240 : 72;
    }

    return 72;
}

export function getDagreNodeSize(node: Node, direction: LayoutDirectionType): {height: number; width: number} {
    // A graph frame is handed to the engine as a sized leaf: the layout pre-pass has already laid
    // its members out and written the box it needs, so its footprint IS that box on both axes.
    const graphFrame = (node.data as NodeDataType)?.graphFrame;

    if (graphFrame) {
        return {height: graphFrame.height, width: graphFrame.width};
    }

    const height = calculateNodeHeight(node);

    // Triggers render as a ~72px icon box; reserving the full task footprint
    // spaces the trigger row too far apart, so they get a tighter cross-axis
    // size — width in TB (horizontal row), height in LR (vertical column).
    const isTrigger = (node.data as NodeDataType)?.trigger === true && node.id !== TRIGGER_PLACEHOLDER_NODE_ID;

    if (direction === 'LR') {
        const isGhostNode =
            node.type === 'taskDispatcherTopGhostNode' ||
            node.type === 'taskDispatcherBottomGhostNode' ||
            node.type === 'taskDispatcherLeftGhostNode';

        let width = 120;

        if (node.type === 'taskDispatcherTopGhostNode') {
            width = 0;
        } else if (isGhostNode) {
            width = PLACEHOLDER_NODE_HEIGHT;
        } else if (node.type === 'placeholder') {
            width = height;
        } else if (node.type === 'clusterRoot') {
            width = hasConfiguredClusterElements(node) ? 292 : 120;
        }

        return {height: isTrigger ? TRIGGER_NODE_DAGRE_WIDTH : NODE_WIDTH, width};
    }

    if (node.type === 'clusterRoot' && hasConfiguredClusterElements(node)) {
        return {height, width: CLUSTER_ROOT_NODE_WIDTH};
    }

    if (isTrigger) {
        return {height, width: TRIGGER_NODE_DAGRE_WIDTH};
    }

    return {height, width: NODE_WIDTH};
}

export const convertTaskToNode = (
    task: WorkflowTask,
    taskDefinition: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic,
    isTrigger: boolean
): Node => {
    const componentName = task.type.split('/')[0];

    const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(componentName);

    return {
        data: {
            ...task,
            componentName,
            icon: (
                <InlineSVG
                    className="size-9"
                    loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                    src={taskDefinition.icon!}
                />
            ),
            operationName: task.type.split('/')[2],
            taskDispatcher: isTaskDispatcher,
            taskDispatcherId: isTaskDispatcher ? task.name : undefined,
            trigger: isTrigger,
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: task.clusterRoot ? 'clusterRoot' : 'workflow',
    };
};

export const buildTriggerNodes = (
    triggers: WorkflowTrigger[] | undefined,
    componentDefinitions: ComponentDefinitionBasic[],
    canvasWidth: number
): {placeholderNode: Node; triggerNodes: Node[]} => {
    const placeholderNode: Node = {
        data: {label: '+'},
        id: TRIGGER_PLACEHOLDER_NODE_ID,
        position: {x: 0, y: 0},
        type: 'triggerPlaceholder',
    };

    if (!triggers || triggers.length === 0) {
        return {placeholderNode, triggerNodes: [createDefaultNodes(canvasWidth)[0]]};
    }

    const triggerNodes = triggers.map((trigger) => {
        const componentName = trigger.type.split('/')[0];

        const triggerDefinition = componentDefinitions.find((definition) => definition.name === componentName);

        if (triggerDefinition) {
            return convertTaskToNode(trigger, triggerDefinition, true);
        }

        return {
            data: {
                ...trigger,
                componentName,
                icon: <ComponentIcon className="size-9 flex-none text-gray-900" />,
                operationName: trigger.type.split('/')[2],
                trigger: true,
                workflowNodeName: trigger.name,
            },
            id: trigger.name,
            position: {x: 0, y: 0},
            type: 'workflow',
        } as Node;
    });

    return {placeholderNode, triggerNodes};
};

export const positionTriggerPlaceholder = (nodes: Node[], direction: LayoutDirectionType): void => {
    const placeholderNode = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID);

    if (!placeholderNode) {
        return;
    }

    const triggerNodes = nodes.filter((node) => node.data?.trigger === true && node.id !== TRIGGER_PLACEHOLDER_NODE_ID);

    if (triggerNodes.length === 0) {
        return;
    }

    // The slot sits one gap past what the trigger occupies, in both directions, so its connector
    // is the same short length either way. Matching the wider interval the triggers sit apart
    // stretched the LR connector to roughly twice its TB counterpart.
    const isVertical = direction === 'LR';

    const lastTrigger = triggerNodes.reduce((furthest, node) =>
        (isVertical ? node.position.y > furthest.position.y : node.position.x > furthest.position.x) ? node : furthest
    );

    // What a trigger visually occupies along the row's axis. In LR that is the icon box plus the
    // label lines stacked beneath it — a fixed three-line block, since the label wraps under the
    // icon there. In TB the label runs to the RIGHT of the icon and its length is the trigger's
    // own, so the extent is estimated from its label texts. No layout constant can stand in for
    // it: the label's container is min-w-max with visible overflow, so nothing clips it to a
    // footprint. This is the same reach separateTriggerRow keeps the NEXT trigger clear of, so the
    // slot sits where a following trigger would.
    const triggerExtent = isVertical
        ? TRIGGER_NODE_BOX_SIZE + TRIGGER_LABEL_BLOCK_HEIGHT
        : TRIGGER_NODE_BOX_SIZE + getLabelCrossOverhang(lastTrigger);

    if (isVertical) {
        placeholderNode.position = {
            // Centred on the trigger's icon box. The slot's own box is inset by its horizontal
            // margin, so centring has to account for that as well as the size difference.
            x:
                lastTrigger.position.x +
                (TRIGGER_NODE_BOX_SIZE - TRIGGER_PLACEHOLDER_NODE_SIZE) / 2 -
                TRIGGER_PLACEHOLDER_BOX_MARGIN,
            y: lastTrigger.position.y + triggerExtent + TRIGGER_PLACEHOLDER_GAP,
        };
    } else {
        placeholderNode.position = {
            x: lastTrigger.position.x + triggerExtent + TRIGGER_PLACEHOLDER_GAP,
            // Vertically center the "+" box on the 72px icon box (node content is center-aligned,
            // so the box starts at the node's y position).
            y: lastTrigger.position.y + (TRIGGER_NODE_BOX_SIZE - TRIGGER_PLACEHOLDER_NODE_SIZE) / 2,
        };
    }
};

export interface GetLayoutElementsProps {
    canvasHeight?: number;
    canvasWidth: number;
    direction?: LayoutDirectionType;
    edges: Edge[];
    nodes: Node[];
    savedPositionCrossAxisShift?: number;
}

export interface LayoutElementsResultI {
    edges: Edge[];
    // The engine that actually produced this layout — ELK falls back to dagre on
    // unsupported shapes and layout errors, so geometry-coupled renderers (the LR
    // ring-bar handle flip) key on this instead of the selected engine.
    engine: 'dagre' | 'elk';
    nodes: Node[];
}

export const getClusterElementsLayoutElements = ({
    canvasHeight,
    canvasWidth,
    currentRootPosition,
    edges,
    nodes,
}: {
    canvasHeight: number;
    canvasWidth: number;
    currentRootPosition?: {x: number; y: number};
    edges: Edge[];
    nodes: Node[];
}): {edges: Edge[]; nodes: Node[]} => {
    const mainRootNode = nodes.find((node) => node.data.clusterElements && !node.parentId);

    if (!mainRootNode) {
        console.error('Main root node not found');

        return {edges, nodes};
    }

    const placeholderNodes = nodes.filter((node) => node.type === 'placeholder');
    const workflowNodes = nodes.filter((node) => node.type !== 'placeholder');

    if (workflowNodes.length === 0) {
        console.error('Cluster element workflow nodes not found');

        return {edges, nodes: placeholderNodes};
    }

    const mainRootTypesCount = (mainRootNode.data.clusterElementTypesCount as number) || 1;
    const mainRootWidth = calculateNodeWidth(mainRootTypesCount) || ROOT_CLUSTER_WIDTH;
    const canvasCenterX = canvasWidth / DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM / 2;

    const positionedNodes: Node[] = [];

    positionedNodes.push({
        ...mainRootNode,
        position: {
            x: canvasCenterX - mainRootWidth / 2,
            y: NODE_HEIGHT,
        },
    });

    const placeholderY = 160;
    const childBaseY = placeholderY + PLACEHOLDER_NODE_HEIGHT + NODE_HEIGHT / 4;
    const horizontalGap = CLUSTER_ELEMENT_NODE_WIDTH + CLUSTER_ELEMENT_GAP;

    const overlapPadding = CLUSTER_ELEMENT_OVERLAP_PADDING;

    // Labels rendered below node circles extend ~40-60px beyond NODE_HEIGHT.
    // Include this overhang in extent calculations so stacked cluster roots
    // don't overlap labels from sibling subtrees.
    const labelOverhang = 40;

    // Returns the maximum Y extent of the subtree (relative to the parent)
    const positionChildrenOfParent = (parentId: string): number => {
        const children = workflowNodes.filter((node) => node.parentId === parentId && node.id !== parentId);

        if (children.length === 0) {
            return NODE_HEIGHT + labelOverhang;
        }

        const parentTypesCount = (children[0].data.parentClusterRootElementsTypeCount as number) || 1;
        const parentWidth = calculateNodeWidth(parentTypesCount) || ROOT_CLUSTER_WIDTH;
        const parentCenterX = parentWidth / 2;

        const typeGroups = new Map<string, Node[]>();

        for (const child of children) {
            const childType = child.data.clusterElementType as string;

            if (!typeGroups.has(childType)) {
                typeGroups.set(childType, []);
            }

            typeGroups.get(childType)!.push(child);
        }

        let maxExtentY = NODE_HEIGHT;

        // Position all children (regular and cluster root) in type order as
        // defined by the parent cluster root handles. Overlap resolution will
        // shift nodes right when they collide with siblings.
        const sortedTypeGroups = [...typeGroups.values()].sort(
            (groupA, groupB) =>
                ((groupA[0].data.clusterElementTypeIndex as number) || 0) -
                ((groupB[0].data.clusterElementTypeIndex as number) || 0)
        );

        for (const typeChildren of sortedTypeGroups) {
            const typeIndex = (typeChildren[0].data.clusterElementTypeIndex as number) || 0;

            const handleX = getHandlePosition({
                handlesCount: parentTypesCount,
                index: typeIndex,
                nodeWidth: parentWidth,
            });

            const isRightSide = handleX >= parentCenterX;

            const regularChildren = typeChildren.filter((child) => !child.data.isNestedClusterRoot);
            const clusterRootChildren = typeChildren.filter((child) => child.data.isNestedClusterRoot);

            const positionedRegularSiblings: {position: {x: number}}[] = [];

            for (let childIndex = 0; childIndex < regularChildren.length; childIndex++) {
                const child = regularChildren[childIndex];

                if (containsNodePosition(child.data.metadata)) {
                    const savedPosition = child.data.metadata.ui.nodePosition;

                    positionedNodes.push({...child, position: savedPosition});
                    positionedRegularSiblings.push({position: savedPosition});
                } else {
                    const firstChildX = handleX - CLUSTER_ELEMENT_NODE_WIDTH / 2;
                    let childX: number;

                    if (positionedRegularSiblings.length === 0) {
                        childX = firstChildX;
                    } else if (isRightSide) {
                        const maxSiblingX = Math.max(...positionedRegularSiblings.map((sibling) => sibling.position.x));

                        childX = maxSiblingX + horizontalGap;
                    } else {
                        const minSiblingX = Math.min(...positionedRegularSiblings.map((sibling) => sibling.position.x));

                        childX = minSiblingX - horizontalGap;
                    }

                    positionedNodes.push({...child, position: {x: childX, y: childBaseY}});
                    positionedRegularSiblings.push({position: {x: childX}});
                }

                const childExtent = positionChildrenOfParent(child.id);

                maxExtentY = Math.max(maxExtentY, childBaseY + childExtent);
            }

            const positionedClusterRootSiblings: {position: {x: number}; width: number}[] = [];

            for (let childIndex = 0; childIndex < clusterRootChildren.length; childIndex++) {
                const child = clusterRootChildren[childIndex];
                const childTypesCount = (child.data.clusterElementTypesCount as number) || 1;
                const childWidth = calculateNodeWidth(childTypesCount) || ROOT_CLUSTER_WIDTH;

                if (containsNodePosition(child.data.metadata)) {
                    const savedPosition = child.data.metadata.ui.nodePosition;

                    positionedNodes.push({...child, position: savedPosition});
                    positionedClusterRootSiblings.push({position: savedPosition, width: childWidth});
                } else {
                    const firstChildX = handleX - childWidth / 2;
                    let childX: number;

                    if (positionedClusterRootSiblings.length === 0) {
                        childX = firstChildX;
                    } else if (isRightSide) {
                        const rightmostSibling = positionedClusterRootSiblings.reduce(
                            (rightmost, sibling) =>
                                sibling.position.x + sibling.width > rightmost.position.x + rightmost.width
                                    ? sibling
                                    : rightmost,
                            positionedClusterRootSiblings[0]
                        );

                        childX = rightmostSibling.position.x + rightmostSibling.width + CLUSTER_ROOT_GAP;
                    } else {
                        const leftmostSibling = positionedClusterRootSiblings.reduce(
                            (leftmost, sibling) => (sibling.position.x < leftmost.position.x ? sibling : leftmost),
                            positionedClusterRootSiblings[0]
                        );

                        childX = leftmostSibling.position.x - childWidth - CLUSTER_ROOT_GAP;
                    }

                    positionedNodes.push({...child, position: {x: childX, y: childBaseY}});
                    positionedClusterRootSiblings.push({position: {x: childX}, width: childWidth});
                }

                const childExtent = positionChildrenOfParent(child.id);

                maxExtentY = Math.max(maxExtentY, childBaseY + childExtent);
            }
        }

        return maxExtentY;
    };

    positionChildrenOfParent(mainRootNode.id);

    // Resolve horizontal overlaps among sibling nodes
    const siblingGroups = new Map<string, Node[]>();

    for (const node of positionedNodes) {
        const parentId = node.parentId || '';

        if (!siblingGroups.has(parentId)) {
            siblingGroups.set(parentId, []);
        }

        siblingGroups.get(parentId)!.push(node);
    }

    for (const [, siblings] of siblingGroups) {
        if (siblings.length < 2) {
            continue;
        }

        siblings.sort((nodeA, nodeB) => {
            const typeIndexA = (nodeA.data.clusterElementTypeIndex as number) ?? 0;
            const typeIndexB = (nodeB.data.clusterElementTypeIndex as number) ?? 0;

            if (typeIndexA !== typeIndexB) {
                return typeIndexA - typeIndexB;
            }

            return nodeA.position.x - nodeB.position.x;
        });

        const spanBefore = siblings[siblings.length - 1].position.x - siblings[0].position.x;

        for (let i = 0; i < siblings.length; i++) {
            const nodeA = siblings[i];
            const isClusterRootA = !!nodeA.data.clusterElementTypesCount;
            const widthA = isClusterRootA
                ? calculateNodeWidth(nodeA.data.clusterElementTypesCount as number) || ROOT_CLUSTER_WIDTH
                : CLUSTER_ELEMENT_NODE_WIDTH;

            for (let j = i + 1; j < siblings.length; j++) {
                const nodeB = siblings[j];

                if (containsNodePosition(nodeB.data.metadata)) {
                    continue;
                }

                const isClusterRootB = !!nodeB.data.clusterElementTypesCount;

                // Small circle nodes (72px) have labels that extend beyond
                // each side; account for this to prevent label overlap
                const labelPaddingA = isClusterRootA ? 0 : CLUSTER_ELEMENT_LABEL_PADDING;
                const labelPaddingB = isClusterRootB ? 0 : CLUSTER_ELEMENT_LABEL_PADDING;

                // When both nodes are cluster roots, enforce the intended gap
                // so overlap-resolution cascades don't compress spacing below
                // the designed CLUSTER_ROOT_GAP.
                const minGap = isClusterRootA && isClusterRootB ? CLUSTER_ROOT_GAP : overlapPadding;

                const verticalOverlap = Math.abs(nodeA.position.y - nodeB.position.y) < NODE_HEIGHT + labelOverhang;
                const minX = nodeA.position.x + widthA + labelPaddingA + labelPaddingB + minGap;

                if (verticalOverlap && nodeB.position.x < minX) {
                    nodeB.position = {...nodeB.position, x: minX};
                }
            }
        }

        // Redistribute expansion evenly so the group stays centered
        // under the parent instead of only growing right.
        const spanAfter = siblings[siblings.length - 1].position.x - siblings[0].position.x;
        const expansion = spanAfter - spanBefore;

        if (expansion > 0) {
            const leftShift = expansion / 2;

            for (const sibling of siblings) {
                if (!containsNodePosition(sibling.data.metadata)) {
                    sibling.position = {...sibling.position, x: sibling.position.x - leftShift};
                }
            }
        }
    }

    // Resolve overlaps ACROSS subtrees. The pass above only compares nodes sharing a parent, but two nested cluster
    // roots sitting side by side each position their own children independently, so a child of one can land on top of
    // a child of the other. Positions are parent-relative, so the comparison has to happen in absolute coordinates.
    // Rows are processed top-down: by the time a row is swept, every ancestor row has already settled, so a parent's
    // shift is included in its children's absolute positions and the whole subtree moves with it.
    const getNodeWidth = (node: Node): number =>
        node.data.clusterElementTypesCount
            ? calculateNodeWidth(node.data.clusterElementTypesCount as number) || ROOT_CLUSTER_WIDTH
            : CLUSTER_ELEMENT_NODE_WIDTH;

    const getAbsolutePoint = (node: Node): {x: number; y: number} => {
        let absoluteX = node.position.x;
        let absoluteY = node.position.y;
        let ancestorId = node.parentId;

        while (ancestorId) {
            const ancestor = positionedNodes.find((positioned) => positioned.id === ancestorId);

            if (!ancestor) {
                break;
            }

            absoluteX += ancestor.position.x;
            absoluteY += ancestor.position.y;
            ancestorId = ancestor.parentId;
        }

        return {x: absoluteX, y: absoluteY};
    };

    const getAbsoluteX = (node: Node): number => getAbsolutePoint(node).x;

    // Rows are keyed by ABSOLUTE y. Every node's relative y is the same childBaseY regardless of depth, so keying by
    // the relative value would collapse every depth into a single row and compare nodes that never share a line.
    const nodesByRow = new Map<number, Node[]>();

    for (const node of positionedNodes) {
        if (!node.parentId) {
            continue;
        }

        const row = Math.round(getAbsolutePoint(node).y);

        if (!nodesByRow.has(row)) {
            nodesByRow.set(row, []);
        }

        nodesByRow.get(row)!.push(node);
    }

    for (const row of [...nodesByRow.keys()].sort((rowA, rowB) => rowA - rowB)) {
        const rowNodes = nodesByRow.get(row)!;

        if (rowNodes.length < 2) {
            continue;
        }

        const placements = rowNodes
            .map((node) => ({absoluteX: getAbsoluteX(node), node, width: getNodeWidth(node)}))
            .sort((placementA, placementB) => placementA.absoluteX - placementB.absoluteX);

        for (let index = 1; index < placements.length; index++) {
            const previous = placements[index - 1];
            const current = placements[index];

            if (containsNodePosition(current.node.data.metadata)) {
                continue;
            }

            const previousLabelPadding = previous.node.data.clusterElementTypesCount
                ? 0
                : CLUSTER_ELEMENT_LABEL_PADDING;
            const currentLabelPadding = current.node.data.clusterElementTypesCount ? 0 : CLUSTER_ELEMENT_LABEL_PADDING;
            const minGap =
                previous.node.data.clusterElementTypesCount && current.node.data.clusterElementTypesCount
                    ? CLUSTER_ROOT_GAP
                    : overlapPadding;

            const minAbsoluteX =
                previous.absoluteX + previous.width + previousLabelPadding + currentLabelPadding + minGap;

            if (current.absoluteX < minAbsoluteX) {
                const shift = minAbsoluteX - current.absoluteX;

                current.node.position = {...current.node.position, x: current.node.position.x + shift};
                current.absoluteX = minAbsoluteX;
            }
        }
    }

    // Center the graph on the canvas.
    if (positionedNodes.length === 1) {
        const viewportWidth = canvasWidth / DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM;
        const viewportHeight = canvasHeight / DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM;
        const rootPadding = NODE_HEIGHT;

        const centeredX = currentRootPosition ? currentRootPosition.x : viewportWidth / 2 - mainRootWidth / 2;
        const centeredY = viewportHeight / 2 - NODE_HEIGHT / 2 - placeholderY / 2;

        positionedNodes[0].position = {
            x: Math.max(rootPadding, Math.min(centeredX, viewportWidth - mainRootWidth - rootPadding)),
            y: Math.max(rootPadding, Math.min(centeredY, viewportHeight - NODE_HEIGHT - rootPadding)),
        };
    } else if (positionedNodes.length > 1) {
        const absolutePositionMap = new Map<string, {x: number; y: number}>();

        const getAbsolutePosition = (nodeId: string): {x: number; y: number} => {
            const cached = absolutePositionMap.get(nodeId);

            if (cached) {
                return cached;
            }

            const node = positionedNodes.find((positioned) => positioned.id === nodeId);

            if (!node || !node.parentId) {
                const position = node?.position ?? {x: 0, y: 0};

                absolutePositionMap.set(nodeId, position);

                return position;
            }

            const parentAbsolutePosition = getAbsolutePosition(node.parentId);

            const absolutePosition = {
                x: parentAbsolutePosition.x + node.position.x,
                y: parentAbsolutePosition.y + node.position.y,
            };

            absolutePositionMap.set(nodeId, absolutePosition);

            return absolutePosition;
        };

        let graphMinX = Infinity;
        let graphMaxX = -Infinity;
        let graphMinY = Infinity;
        let graphMaxY = -Infinity;

        for (const node of positionedNodes) {
            const absolutePosition = getAbsolutePosition(node.id);
            const isClusterRoot = !!node.data.clusterElementTypesCount;

            const nodeWidth = isClusterRoot
                ? calculateNodeWidth(node.data.clusterElementTypesCount as number) || ROOT_CLUSTER_WIDTH
                : CLUSTER_ELEMENT_NODE_WIDTH;

            graphMinX = Math.min(graphMinX, absolutePosition.x);
            graphMaxX = Math.max(graphMaxX, absolutePosition.x + nodeWidth);
            graphMinY = Math.min(graphMinY, absolutePosition.y);
            graphMaxY = Math.max(graphMaxY, absolutePosition.y + NODE_HEIGHT);
        }

        const viewportWidth = canvasWidth / DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM;
        const viewportHeight = canvasHeight / DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM;
        const canvasCenterX = viewportWidth / 2;
        const canvasCenterY = viewportHeight / 2;
        const graphCenterX = (graphMinX + graphMaxX) / 2;
        const graphCenterY = (graphMinY + graphMaxY) / 2;
        const graphHeight = graphMaxY - graphMinY;
        const rootPadding = NODE_HEIGHT;

        // When currentRootPosition is provided (re-layout after drag), preserve
        // the horizontal position to avoid shifting the graph. Always recompute
        // vertical position since it must adapt as the graph grows.
        const horizontalShift = currentRootPosition ? 0 : canvasCenterX - graphCenterX;

        // Only center vertically when the graph fits within the viewport.
        // When the graph is taller, keep the root near the top so it's visible.
        const verticalShift = graphHeight < viewportHeight ? canvasCenterY - graphCenterY : 0;

        const mainRootPositioned = positionedNodes.find((node) => node.id === mainRootNode.id);

        if (mainRootPositioned) {
            const newRootX = currentRootPosition
                ? currentRootPosition.x
                : mainRootPositioned.position.x + horizontalShift;
            const newRootY = mainRootPositioned.position.y + verticalShift;

            mainRootPositioned.position = {
                x: Math.max(rootPadding, Math.min(newRootX, viewportWidth - mainRootWidth - rootPadding)),
                y: Math.max(rootPadding, Math.min(newRootY, viewportHeight - NODE_HEIGHT - rootPadding)),
            };
        }
    }

    return {
        edges,
        nodes: [...positionedNodes, ...placeholderNodes],
    };
};

/**
 * Engine-independent edge post-processing shared by the dagre and ELK layout
 * paths: prioritizes task edges over ghost/placeholder edges per source, keeps
 * a single edge per source unless the source legitimately fans out (ghosts,
 * cluster roots, branch, fork-join), dedupes by endpoint+handle key, and drops
 * edges referencing nodes that no longer exist.
 */
export function filterAndDedupeLayoutEdges(allNodes: Node[], edges: Edge[]): Edge[] {
    // BACKSTOP, not a live path. A graph frame's own routes (`graphTransition`, `graphStart`) do
    // not reach here from the editor: `layoutGraphFrames` partitions them out before the outer
    // layout runs and `useLayout` appends them to the result afterwards, so they skip this
    // function entirely. This guard exists for direct callers, the same defence-in-depth the two
    // engine entry points already carry (`getLayoutElements`'s pre-dagre filter and
    // `buildElkGraph`'s skip). Without it such a caller would silently lose routes: a route
    // shares its source with that member's own structural wiring, so the "one edge per source"
    // collapse below would treat the two as competing and keep only one. Routes still go through
    // the dedupe and dangling-reference passes.
    const isGraphFrameRoute = (edge: Edge) =>
        edge.type === GRAPH_TRANSITION_EDGE_TYPE || edge.type === GRAPH_START_EDGE_TYPE;

    const graphFrameRouteEdges = edges.filter(isGraphFrameRoute);
    const structuralEdges = edges.filter((edge) => !isGraphFrameRoute(edge));

    const sourceEdgeMap = new Map<string, Edge[]>();

    // Sort edges to prioritize task connections over ghost connections
    const sortedEdges = [...structuralEdges].sort((firstEdge, secondEdge) => {
        const isFirstEdgeToAuxiliaryNode =
            firstEdge.target.includes('ghost') || firstEdge.target.includes('placeholder');

        const isSecondEdgeToAuxiliaryNode =
            secondEdge.target.includes('ghost') || secondEdge.target.includes('placeholder');

        if (isFirstEdgeToAuxiliaryNode && !isSecondEdgeToAuxiliaryNode) {
            return 1;
        }

        if (!isFirstEdgeToAuxiliaryNode && isSecondEdgeToAuxiliaryNode) {
            return -1;
        }

        return 0;
    });

    // Group edges by source
    sortedEdges.forEach((edge) => {
        if (!sourceEdgeMap.has(edge.source)) {
            sourceEdgeMap.set(edge.source, []);
        }

        sourceEdgeMap.get(edge.source)?.push(edge);
    });

    const filteredEdges: Edge[] = [...graphFrameRouteEdges];

    // Filter edges so that only one edge is kept for each source node
    sourceEdgeMap.forEach((sourceEdges, source) => {
        const sourceNode = allNodes.find((node) => node.id === source);

        if (sourceEdges.length === 0 || !sourceNode) {
            return;
        }

        const multipleEdgesAllowed = [
            {
                condition: sourceNode.type === 'taskDispatcherTopGhostNode',
            },
            {
                condition: sourceNode.type === 'taskDispatcherBottomGhostNode',
            },
            {
                condition: sourceNode.data.clusterRoot,
            },
            {
                condition: sourceNode.data.componentName === 'branch',
            },
            {
                condition: sourceNode.data.componentName === 'fork-join',
            },
        ];

        if (multipleEdgesAllowed.some(({condition}) => condition)) {
            filteredEdges.push(...sourceEdges);
        } else {
            filteredEdges.push(sourceEdges[0]);
        }
    });

    const dedupedEdges = filteredEdges.reduce(
        (uniqueEdges: {edges: Edge[]; map: Map<string, boolean>}, edge: Edge) => {
            const {source, target} = edge;

            const targetHandle = edge.targetHandle ? `-${edge.targetHandle}` : '';
            const sourceHandle = edge.sourceHandle ? `-${edge.sourceHandle}` : '';

            const edgeKey = `${source}=>${target}${targetHandle}${sourceHandle}`;

            if (!uniqueEdges.map.has(edgeKey)) {
                uniqueEdges.map.set(edgeKey, true);

                uniqueEdges.edges.push(edge);
            }

            return uniqueEdges;
        },
        {edges: [], map: new Map<string, boolean>()}
    ).edges;

    // Remove edges that reference non-existent nodes
    const nodeIds = new Set(allNodes.map((node) => node.id));

    return dedupedEdges.filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target));
}

export const getLayoutElements = async ({
    canvasHeight,
    canvasWidth,
    direction = 'TB',
    edges,
    nodes,
    savedPositionCrossAxisShift = 0,
}: GetLayoutElementsProps) => {
    // `graphTransition` and `graphStart` edges must never reach dagre or this function's
    // post-dagre chain-walker pipeline: they are free-form routes between members inside a
    // `graphFrame`, not chain links, and a cyclic pair of transitions would corrupt dagre's own
    // ranking while a start edge would walk the chain straight into the frame. `layoutGraphFrames`
    // already strips both before the outer layout runs, so this is a backstop for direct callers
    // (and for getElkLayoutElements's error-fallback branch, which re-invokes this function with
    // the SAME edges array ELK was given).
    edges = edges.filter((edge) => edge.type !== GRAPH_TRANSITION_EDGE_TYPE && edge.type !== GRAPH_START_EDGE_TYPE);

    const dagreModule = await loadDagre();

    const dagreGraph = new dagreModule.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

    const effectiveDirection = direction;

    dagreGraph.setGraph({
        nodesep: 50,
        rankdir: effectiveDirection,
    });

    nodes.forEach((node) => {
        dagreGraph.setNode(node.id, getDagreNodeSize(node, effectiveDirection));
    });

    edges.forEach((edge) => {
        if ((edge.data as Record<string, unknown> | undefined)?.triggerFanIn) {
            // Span extra ranks so the trigger row sits a full standard gap above the
            // horizontal bus AND the bus sits a full standard gap above the first
            // task. The bus itself is pinned a fixed short distance below the trigger
            // row by the edge components (TRIGGER_FAN_IN_BUS_OFFSET), so this rank
            // span only sizes the lower (bus→first-task) leg to one task-to-task gap.
            dagreGraph.setEdge(edge.source, edge.target, {minlen: 2});
        } else if (edge.target.includes('bottom-ghost')) {
            // A graph's ghosts carry no branch rails and paint no bar, so the extra rank every
            // other dispatcher needs to clear its own chrome is dead space here.
            dagreGraph.setEdge(edge.source, edge.target, {
                minlen: edge.target.includes(GRAPH_BOTTOM_GHOST_SUFFIX) ? 1 : 2,
            });
        } else if (edge.target.includes('top-ghost')) {
            dagreGraph.setEdge(edge.source, edge.target, {minlen: 1});
        } else {
            const sourceNode = nodes.find((node) => node.id === edge.source);

            const hasValidClusterElements = !!sourceNode && hasConfiguredClusterElements(sourceNode);

            let edgeLength = 1;

            if (hasValidClusterElements && effectiveDirection !== 'LR') {
                edgeLength = 2;
            }

            // Edges from top ghosts to content children need extra space
            // so the edge add-button (+) has room between the case label and the node.
            if (sourceNode?.type === 'taskDispatcherTopGhostNode') {
                edgeLength = edge.source.includes(GRAPH_TOP_GHOST_SUFFIX) ? 1 : 2;
            }

            dagreGraph.setEdge(edge.source, edge.target, {minlen: edgeLength});
        }
    });

    dagreModule.layout(dagreGraph, {disableOptimalOrderHeuristic: true});

    const crossAxis = getCrossAxis(direction);
    const crossAxisSize = getCrossAxisNodeSize(direction);

    const canvasCrossDimension = direction === 'LR' && canvasHeight ? canvasHeight : canvasWidth;

    const triggerCrossHalf = direction === 'LR' ? NODE_WIDTH / 2 : 72 / 2;

    // Center the graph on the midpoint of the entry (trigger) row rather than the
    // first trigger. With multiple triggers, anchoring on nodes[0] (the leftmost
    // trigger) pushes the whole row off to one side; the row midpoint keeps it
    // centered and collapses to the old behavior when there is a single trigger.
    const entryNodeCrossPositions = nodes
        .filter((node) => (node.data as NodeDataType)?.trigger === true && node.id !== TRIGGER_PLACEHOLDER_NODE_ID)
        .map((node) => dagreGraph.node(node.id)[crossAxis]);

    const entryAnchorCross =
        entryNodeCrossPositions.length > 0
            ? (Math.min(...entryNodeCrossPositions) + Math.max(...entryNodeCrossPositions)) / 2
            : dagreGraph.node(nodes[0].id)[crossAxis];

    const canvasCenteringOffset = canvasCrossDimension / 2 - entryAnchorCross - triggerCrossHalf;

    const allNodes = nodes.map((node) => {
        const dagreNode = dagreGraph.node(node.id);
        let crossAxisPosition = dagreNode[crossAxis] + canvasCenteringOffset;

        const hasValidClusterElements = hasConfiguredClusterElements(node);

        if (hasValidClusterElements && node.data.clusterRoot && direction === 'TB') {
            crossAxisPosition -= 85;
        }

        // Same compensation the configured cluster root gets above, computed rather than
        // hard-coded: dagre reports the footprint CENTRE on the cross axis, and the convention
        // downstream is that a node's chain axis sits half a 72px icon box past its position. A
        // frame paints its whole box, so its own centre has to land on that axis instead.
        const graphFrame = (node.data as NodeDataType).graphFrame;

        if (graphFrame) {
            crossAxisPosition -= (direction === 'TB' ? graphFrame.width : graphFrame.height) / 2 - NODE_ANCHOR_HALF;
        }

        const mainAxis = direction === 'TB' ? 'y' : 'x';
        const mainAxisPosition = dagreNode[mainAxis] - getRenderedMainAxisSize(node, direction) / 2;

        if (hasValidClusterElements && node.data.clusterRoot && direction === 'LR') {
            crossAxisPosition -= 23;
        }

        return {
            ...node,
            position: {
                [crossAxis]: crossAxisPosition,
                [mainAxis]: mainAxisPosition,
            } as {x: number; y: number},
        };
    });

    // Post-dagre constraint pipeline
    const nodesep = 50;
    const conditionCaseOffset = (crossAxisSize + nodesep) / 2;

    constrainConditionGhostsCrossAxis(allNodes, crossAxis);
    constrainOnErrorGhostsCrossAxis(allNodes, crossAxis);
    constrainBranchGhostsCrossAxis(allNodes, crossAxis);
    alignBranchCaseChildren(allNodes, edges, crossAxis, crossAxisSize);
    centerNodesAfterBottomGhost(allNodes, edges, {crossAxis, crossAxisSize, direction});
    alignDispatcherGhostsCrossAxis(allNodes, crossAxis);
    separateOverlappingConditionChildren(allNodes, edges, crossAxis);
    separateOverlappingOnErrorChildren(allNodes, edges, crossAxis);
    alignSimpleConditionChildren(allNodes, edges, {conditionCaseOffset, crossAxis, nodesep});
    pullSimpleOnErrorChildrenInward(allNodes, edges, {conditionCaseOffset, crossAxis});
    positionConditionCasePlaceholders(allNodes, {conditionCaseOffset, crossAxis});
    positionOnErrorCasePlaceholders(allNodes, {conditionCaseOffset, crossAxis});
    shiftConditionBranchContent(allNodes, {crossAxis, nodesep});
    shiftOnErrorBranchContent(allNodes, {crossAxis, nodesep});
    constrainLeftGhostPositions(allNodes, {conditionCaseOffset, crossAxis, direction});

    if (direction === 'LR') {
        centerLRSmallNodes(allNodes, crossAxis);
    }

    positionTriggerPlaceholder(allNodes, direction);

    const mainAxis = direction === 'TB' ? 'y' : 'x';

    const savedDispatcherDeltas = applySavedPositions(allNodes, crossAxis, savedPositionCrossAxisShift);

    adjustBottomGhostForMovedChildren(allNodes, edges, mainAxis, direction, savedDispatcherDeltas);

    const chainDeltas = alignChainNodesCrossAxis(allNodes, edges, crossAxis, direction, savedDispatcherDeltas);
    const allDispatcherDeltas = new Map([...savedDispatcherDeltas, ...chainDeltas]);

    alignTrailingPlaceholder(allNodes, edges, crossAxis, direction, allDispatcherDeltas);

    centerDispatcherPlaceholdersOnMainAxis(allNodes, edges, mainAxis);

    if (direction === 'TB') {
        centerDispatcherChildrenOnMainAxis(allNodes, edges, mainAxis);
    }

    edges = filterAndDedupeLayoutEdges(allNodes, edges);

    // `engine` reports which engine actually produced this layout — getElkLayoutElements
    // returns this function's result verbatim from its error fallback, so consumers that
    // couple rendering to engine geometry (the LR ring-bar handle flip) can trust it.
    return {edges, engine: 'dagre' as const, nodes: allNodes};
};

interface CreateEdgeFromTaskDispatcherBottomGhostNodeProps {
    allNodes?: Node[];
    index?: number;
    node: Node;
    tasks?: WorkflowTask[];
}

export const createEdgeFromTaskDispatcherBottomGhostNode = ({
    allNodes = [],
    index = 0,
    node,
    tasks = [],
}: CreateEdgeFromTaskDispatcherBottomGhostNodeProps): Edge | null => {
    const nodeData = node.data as NodeDataType;

    const {taskDispatcherId} = nodeData;

    if (!taskDispatcherId) {
        return null;
    }

    let componentName;

    // Connect to the parent task dispatcher if this is a nested task dispatcher
    if (node.data.isNestedBottomGhost) {
        const parentTaskDispatcher = getParentTaskDispatcherTask(taskDispatcherId, tasks);

        if (!parentTaskDispatcher) {
            return null;
        }

        const taskDispatcherNode = allNodes.find((node) => node.id === taskDispatcherId);

        componentName = parentTaskDispatcher.type.split('/')[0];

        let parentSubtasks: WorkflowTask[];

        switch (componentName) {
            case 'branch': {
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    context: {
                        caseKey: (taskDispatcherNode?.data as NodeDataType)?.branchData?.caseKey,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            }
            case 'parallel':
                return null;
            case 'condition':
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    context: {
                        conditionCase:
                            ((taskDispatcherNode?.data as NodeDataType).conditionData?.conditionCase as
                                'caseTrue' | 'caseFalse') || CONDITION_CASE_TRUE,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            case 'on-error':
                parentSubtasks = TASK_DISPATCHER_CONFIG['on-error'].getSubtasks({
                    context: {
                        onErrorCase:
                            ((taskDispatcherNode?.data as NodeDataType).onErrorData?.onErrorCase as
                                typeof ON_ERROR_MAIN_BRANCH | typeof ON_ERROR_ERROR_BRANCH) || ON_ERROR_MAIN_BRANCH,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            case 'fork-join': {
                const branches = parentTaskDispatcher.parameters?.branches || [];

                const branchIndex = branches.findIndex(
                    (branch: WorkflowTask[]) =>
                        Array.isArray(branch) && branch.some((subtask) => subtask.name === taskDispatcherId)
                );

                parentSubtasks = branchIndex !== -1 ? branches[branchIndex] || [] : [];

                break;
            }
            // A graph's members live free-form inside its frame: nothing chains a member to the
            // next declared one, and nothing wires a member back to the graph's own bottom bar.
            // Routes between members are `graphTransition` edges, built from
            // `parameters.transitions` by createGraphEdges.
            case 'graph':
                return null;
            default: {
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    task: parentTaskDispatcher,
                });

                break;
            }
        }

        const currentSubtaskIndex = parentSubtasks.findIndex((subtask) => subtask.name === taskDispatcherId);

        const nextSubtask = parentSubtasks[currentSubtaskIndex + 1];

        if (nextSubtask) {
            const edgeFromNestedBottomGhostToNextSubtask = {
                id: `${node.id}=>${nextSubtask.name}`,
                source: node.id,
                style: EDGE_STYLES,
                target: nextSubtask.name,
                type: 'workflow',
            };

            return edgeFromNestedBottomGhostToNextSubtask;
        }

        const parentBottomGhostSegment =
            componentName === 'fork-join' ? 'forkJoin' : componentName === 'on-error' ? 'onError' : componentName;

        const parentTaskDispatcherBottomGhostId = `${parentTaskDispatcher.name}-${parentBottomGhostSegment}-bottom-ghost`;
        const parentTaskDispatcherBottomGhost = allNodes.find((node) => node.id === parentTaskDispatcherBottomGhostId);

        if (!parentTaskDispatcherBottomGhost) {
            return null;
        }

        let targetHandle = `${parentTaskDispatcherBottomGhostId}-right`;

        if (componentName === 'condition') {
            const branchSide = getConditionBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'on-error') {
            const branchSide = getOnErrorBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'fork-join') {
            const branchSide = getForkJoinBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'branch') {
            const branchSide = getBranchCaseSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            const handlePosition = branchSide === 'middle' ? 'top' : branchSide;

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${handlePosition}`;
        }

        return {
            id: `${node.id}=>${parentTaskDispatcherBottomGhostId}`,
            source: node.id,
            style: EDGE_STYLES,
            target: parentTaskDispatcherBottomGhostId,
            targetHandle,
            type: 'workflow',
        };
    }

    const subsequentNodes = allNodes.slice(index + 1);

    const nextTaskNodeOutsideTaskDispatcher = subsequentNodes.find((subsequentNode) => {
        if (subsequentNode.type !== 'workflow' && subsequentNode.type !== 'clusterRoot') {
            return false;
        }

        const subsequentNodeData = subsequentNode.data as NodeDataType;

        if (subsequentNodeData.conditionData && subsequentNodeData.conditionData.conditionId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.onErrorData && subsequentNodeData.onErrorData.onErrorId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.loopData && subsequentNodeData.loopData.loopId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.branchData && subsequentNodeData.branchData.branchId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.parallelData && subsequentNodeData.parallelData.parallelId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.eachData && subsequentNodeData.eachData.eachId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.forkJoinData && subsequentNodeData.forkJoinData.forkJoinId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.graphData && subsequentNodeData.graphData.graphId === taskDispatcherId) {
            return false;
        } else if (
            subsequentNodeData.terminateData &&
            subsequentNodeData.terminateData.terminateId === taskDispatcherId
        ) {
            return false;
        }

        for (const task of tasks || []) {
            const componentName = task.type?.split('/')[0];

            if (!TASK_DISPATCHER_NAMES.includes(componentName)) {
                continue;
            }

            const subtasks = TASK_DISPATCHER_CONFIG[componentName as keyof typeof TASK_DISPATCHER_CONFIG].getSubtasks({
                getAllSubtasks: true,
                task,
            });

            if (Array.isArray(subtasks) && subtasks.some((subtask) => subtask.name === subsequentNode.id)) {
                return false;
            }
        }

        return true;
    });

    if (nextTaskNodeOutsideTaskDispatcher) {
        return {
            id: `${node.id}=>${nextTaskNodeOutsideTaskDispatcher.id}`,
            source: node.id,
            style: EDGE_STYLES,
            target: nextTaskNodeOutsideTaskDispatcher.id,
            type: 'workflow',
        };
    }

    return {
        id: `${node.id}=>${FINAL_PLACEHOLDER_NODE_ID}`,
        source: node.id,
        style: EDGE_STYLES,
        target: FINAL_PLACEHOLDER_NODE_ID,
        type: 'placeholder',
    };
};

/**
 * Determines the target handle side for a branch case based on case position
 */
export function getBranchCaseSide(
    taskDispatcherId: string,
    tasks: WorkflowTask[],
    parentBranchId: string
): 'left' | 'middle' | 'right' {
    const parentBranchTask = tasks?.find((task) => task.name === parentBranchId);

    if (!parentBranchTask) {
        return 'right';
    }

    const defaultCase = {
        key: 'default',
        tasks: parentBranchTask.parameters?.default || [],
    };

    const customCases = (parentBranchTask.parameters?.cases || []).map((caseItem: BranchCaseType) => ({
        key: caseItem.key,
        tasks: caseItem.tasks || [],
    }));

    const allCases = [defaultCase, ...customCases];

    const caseIndex = allCases.findIndex((caseItem) =>
        caseItem.tasks.some((task: WorkflowTask) => task.name === taskDispatcherId)
    );

    if (caseIndex === -1) {
        return 'right';
    }

    const isEvenCount = allCases.length % 2 === 0;

    if (isEvenCount) {
        const halfPoint = allCases.length / 2;

        if (caseIndex < halfPoint) {
            return 'left';
        } else {
            return 'right';
        }
    } else {
        const middleIndex = Math.floor(allCases.length / 2);

        if (caseIndex < middleIndex) {
            return 'left';
        } else if (caseIndex === middleIndex) {
            return 'middle';
        } else {
            return 'right';
        }
    }
}

export function branchCaseKeysMatch(left: string | number, right: string | number): boolean {
    return left === right || String(left) === String(right);
}

/**
 * Collects nested tasks for all task dispatchers in the workflow
 */
export function collectTaskDispatcherData(
    task: WorkflowTask,
    branchChildTasks: BranchChildTasksType,
    conditionChildTasks: ConditionChildTasksType,
    eachChildTasks: EachChildTasksType,
    forkJoinChildTasks: ForkJoinChildTasksType,
    graphChildTasks: GraphChildTasksType,
    loopChildTasks: LoopChildTasksType,
    mapChildTasks: MapChildTasksType,
    onErrorChildTasks: OnErrorChildTasksType,
    parallelChildTasks: ParallelChildTasksType
): void {
    const {name, parameters, type} = task;
    const componentName = type.split('/')[0];

    if (!TASK_DISPATCHER_NAMES.includes(componentName)) {
        return;
    }

    if (componentName === 'condition' && parameters) {
        conditionChildTasks[name] = {
            caseFalse: Array.isArray(parameters.caseFalse)
                ? parameters.caseFalse.map((caseFalseSubtask: WorkflowTask) => caseFalseSubtask.name)
                : [],
            caseTrue: Array.isArray(parameters.caseTrue)
                ? parameters.caseTrue.map((caseTrueSubtask: WorkflowTask) => caseTrueSubtask.name)
                : [],
        };
    } else if (componentName === 'loop' && parameters?.iteratee) {
        loopChildTasks[name] = {
            iteratee: Array.isArray(parameters.iteratee)
                ? parameters.iteratee.map((iteratee: WorkflowTask) => iteratee.name)
                : [],
        };
    } else if (componentName === 'map' && parameters?.iteratee) {
        mapChildTasks[name] = {
            iteratee: Array.isArray(parameters.iteratee)
                ? parameters.iteratee.map((iteratee: WorkflowTask) => iteratee.name)
                : [],
        };
    } else if (componentName === 'on-error' && parameters) {
        const errorBranch = parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH];
        const mainBranch = parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH];

        onErrorChildTasks[name] = {
            mainBranch: Array.isArray(mainBranch) ? mainBranch.map((subtask: WorkflowTask) => subtask.name) : [],
            onErrorBranch: Array.isArray(errorBranch) ? errorBranch.map((subtask: WorkflowTask) => subtask.name) : [],
        };
    } else if (componentName === 'branch' && parameters) {
        branchChildTasks[name] = {
            cases: Array.isArray(parameters.cases)
                ? parameters.cases.reduce((acc: {[key: string]: string[]}, caseItem: BranchCaseType) => {
                      const caseKey = caseItem.key;

                      const taskNames = Array.isArray(caseItem.tasks)
                          ? caseItem.tasks.map((task: WorkflowTask) => task.name)
                          : [];

                      acc[caseKey] = taskNames;

                      return acc;
                  }, {})
                : {},
            default: Array.isArray(parameters.default)
                ? parameters.default.map((defaultSubtask: WorkflowTask) => defaultSubtask.name)
                : [],
        };
    } else if (componentName === 'parallel' && parameters?.tasks) {
        parallelChildTasks[name] = {
            tasks: Array.isArray(parameters.tasks) ? parameters.tasks.map((task: WorkflowTask) => task.name) : [],
        };
    } else if (componentName === 'each' && parameters?.iteratee) {
        eachChildTasks[name] = {
            iteratee: parameters.iteratee.name,
        };
    } else if (componentName === 'fork-join') {
        forkJoinChildTasks[name] = {
            branches: Array.isArray(parameters?.branches)
                ? parameters.branches.map((branch: WorkflowTask[]) =>
                      Array.isArray(branch) ? branch.map((task: WorkflowTask) => task.name) : []
                  )
                : [],
        };
    } else if (componentName === 'graph') {
        graphChildTasks[name] = {
            nodes: Array.isArray(parameters?.nodes) ? parameters.nodes.map((task: WorkflowTask) => task.name) : [],
        };
    }
}

/**
 * Detects if a task is nested inside a task dispatcher and returns relevant nesting data
 */
interface GetTaskAncestryProps {
    branchChildTasks: BranchChildTasksType;
    conditionChildTasks: ConditionChildTasksType;
    eachChildTasks: EachChildTasksType;
    forkJoinChildTasks: ForkJoinChildTasksType;
    graphChildTasks: GraphChildTasksType;
    loopChildTasks: LoopChildTasksType;
    mapChildTasks: MapChildTasksType;
    onErrorChildTasks: OnErrorChildTasksType;
    parallelChildTasks: ParallelChildTasksType;
    taskName: string;
}

export function getTaskAncestry({
    branchChildTasks,
    conditionChildTasks,
    eachChildTasks,
    forkJoinChildTasks,
    graphChildTasks,
    loopChildTasks,
    mapChildTasks,
    onErrorChildTasks,
    parallelChildTasks,
    taskName,
}: GetTaskAncestryProps): {nestingData: Record<string, unknown>; isNested: boolean} {
    let isNested = false;
    let nestingData = {};

    for (const [conditionId, conditionCases] of Object.entries(conditionChildTasks)) {
        const conditionCasesList = [
            {taskNames: conditionCases.caseTrue, value: CONDITION_CASE_TRUE},
            {taskNames: conditionCases.caseFalse, value: CONDITION_CASE_FALSE},
        ];

        const matchingCase = conditionCasesList.find((conditionCase) => conditionCase.taskNames.includes(taskName));

        if (matchingCase) {
            nestingData = {
                conditionData: {
                    conditionCase: matchingCase.value,
                    conditionId,
                    index: matchingCase.taskNames.indexOf(taskName),
                },
            };

            isNested = true;

            break;
        }
    }

    if (!isNested) {
        for (const [onErrorId, onErrorCases] of Object.entries(onErrorChildTasks)) {
            const onErrorCasesList = [
                {taskNames: onErrorCases.mainBranch, value: ON_ERROR_MAIN_BRANCH},
                {taskNames: onErrorCases.onErrorBranch, value: ON_ERROR_ERROR_BRANCH},
            ];

            const matchingOnErrorCase = onErrorCasesList.find((onErrorCase) =>
                onErrorCase.taskNames.includes(taskName)
            );

            if (matchingOnErrorCase) {
                nestingData = {
                    onErrorData: {
                        index: matchingOnErrorCase.taskNames.indexOf(taskName),
                        onErrorCase: matchingOnErrorCase.value,
                        onErrorId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [loopId, loopData] of Object.entries(loopChildTasks)) {
            if (loopData.iteratee.includes(taskName)) {
                nestingData = {
                    loopData: {
                        index: loopData.iteratee.indexOf(taskName),
                        loopId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [mapId, mapData] of Object.entries(mapChildTasks)) {
            if (mapData.iteratee.includes(taskName)) {
                nestingData = {
                    mapData: {
                        index: mapData.iteratee.indexOf(taskName),
                        mapId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [branchId, branchData] of Object.entries(branchChildTasks)) {
            if (branchData.default.includes(taskName)) {
                nestingData = {
                    branchData: {
                        branchId,
                        caseKey: 'default',
                        index: branchData.default.indexOf(taskName),
                    },
                };

                isNested = true;

                break;
            }

            for (const [caseKey, caseTasks] of Object.entries(branchData.cases)) {
                if (caseTasks.includes(taskName)) {
                    nestingData = {
                        branchData: {
                            branchId,
                            caseKey,
                            index: caseTasks.indexOf(taskName),
                        },
                    };

                    isNested = true;

                    break;
                }
            }

            if (isNested) {
                break;
            }
        }
    }

    if (!isNested) {
        for (const [parallelId, parallelData] of Object.entries(parallelChildTasks)) {
            if (parallelData.tasks.includes(taskName)) {
                nestingData = {
                    parallelData: {
                        index: parallelData.tasks.indexOf(taskName),
                        parallelId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [eachId, eachData] of Object.entries(eachChildTasks)) {
            if (eachData.iteratee === taskName) {
                nestingData = {
                    eachData: {
                        eachId,
                        index: 0,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [forkJoinId, forkJoinData] of Object.entries(forkJoinChildTasks)) {
            const forkJoinSubtaskNameBranches = forkJoinData.branches;

            forkJoinSubtaskNameBranches.forEach((branch, branchIndex) => {
                if (isNested) {
                    return;
                }

                const taskIndex = branch.indexOf(taskName);

                if (taskIndex !== -1) {
                    nestingData = {
                        forkJoinData: {
                            branchIndex,
                            forkJoinId,
                            index: taskIndex,
                        },
                    };

                    isNested = true;
                }
            });

            if (isNested) {
                break;
            }
        }
    }

    if (!isNested) {
        for (const [graphId, graphData] of Object.entries(graphChildTasks)) {
            const taskIndex = graphData.nodes.indexOf(taskName);

            if (taskIndex !== -1) {
                nestingData = {graphData: {graphId, index: taskIndex}};
                isNested = true;

                break;
            }
        }
    }

    return {isNested, nestingData};
}

export const createDefaultNodes = (canvasWidth: number, direction: LayoutDirectionType = 'TB'): Node[] => [
    {
        data: {
            componentName: 'manual',
            icon: <PlayIcon className="size-9 text-gray-700" />,
            id: 'manual',
            label: 'Manual',
            name: 'manual',
            operationName: 'manual',
            trigger: true,
            type: 'manual/v1/manual',
            workflowNodeName: 'trigger_1',
        },
        id: 'trigger_1',
        position: direction === 'LR' ? {x: 50, y: canvasWidth / 2 - 36} : {x: canvasWidth / 2 - 36, y: 50},
        type: 'workflow',
    },
    {
        data: {label: '+'},
        id: FINAL_PLACEHOLDER_NODE_ID,
        position: direction === 'LR' ? {x: 150, y: canvasWidth / 2 - 36} : {x: canvasWidth / 2 - 36, y: 150},
        type: 'placeholder',
    },
];

export const createDefaultEdges = (): Edge[] => [
    {
        id: `trigger_1=>${FINAL_PLACEHOLDER_NODE_ID}`,
        source: 'trigger_1',
        target: FINAL_PLACEHOLDER_NODE_ID,
        type: 'placeholder',
    },
];
