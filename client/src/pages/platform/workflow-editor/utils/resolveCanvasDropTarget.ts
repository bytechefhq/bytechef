import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {CanvasDragKindType} from './canvasDragData';

export type CanvasDropTargetType =
    | {edge: Edge; type: 'edge'}
    | {node: Node; type: 'placeholder'}
    | {node: Node; type: 'trigger'}
    | {type: 'triggerPlaceholder'};

interface ResolveCanvasDropTargetProps {
    dragKind: CanvasDragKindType;
    edges: Edge[];
    nodes: Node[];
    target: EventTarget | null;
}

const EDGE_ID_PATTERN = /^.+=>.+$/;

function findClosestEdgeElement(element: Element): Element | undefined {
    let currentElement: Element | null = element;

    while (currentElement) {
        if (
            currentElement.tagName === 'DIV' &&
            EDGE_ID_PATTERN.test(currentElement.id) &&
            !currentElement.id.endsWith('-button')
        ) {
            return currentElement;
        }

        currentElement = currentElement.parentElement;
    }

    return undefined;
}

export default function resolveCanvasDropTarget({
    dragKind,
    edges,
    nodes,
    target,
}: ResolveCanvasDropTargetProps): CanvasDropTargetType | undefined {
    if (!(target instanceof Element)) {
        return undefined;
    }

    const nodeElement = target.closest<HTMLElement>('.react-flow__node');

    const targetNode = nodeElement ? nodes.find((node) => node.id === nodeElement.dataset.id) : undefined;

    if (dragKind === 'trigger') {
        if (targetNode?.type === 'triggerPlaceholder') {
            return {type: 'triggerPlaceholder'};
        }

        if (targetNode && (targetNode.data as NodeDataType).trigger) {
            return {node: targetNode, type: 'trigger'};
        }

        return undefined;
    }

    if (targetNode) {
        const isLaidOut = targetNode.position.x !== 0 || targetNode.position.y !== 0;

        return targetNode.type === 'placeholder' && isLaidOut ? {node: targetNode, type: 'placeholder'} : undefined;
    }

    const edgeElement = findClosestEdgeElement(target);

    const targetEdge = edgeElement ? edges.find((edge) => edge.id === edgeElement.id) : undefined;

    return targetEdge ? {edge: targetEdge, type: 'edge'} : undefined;
}
