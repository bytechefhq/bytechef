import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {GraphTransitionType} from '@/shared/types';
import {Edge} from '@xyflow/react';

export interface SelectedGraphTransitionI {
    graphId: string;
    index: number;
    transition: GraphTransitionType;
}

export function findSelectedGraphTransition(edges: Edge[]): SelectedGraphTransitionI | undefined {
    const selectedTransitionEdge = edges.find((edge) => edge.selected && edge.type === GRAPH_TRANSITION_EDGE_TYPE);

    if (!selectedTransitionEdge) {
        return undefined;
    }

    const {condition, graphId, index, to} = (selectedTransitionEdge.data ?? {}) as {
        condition?: unknown;
        graphId?: unknown;
        index?: unknown;
        to?: unknown;
    };

    if (typeof graphId !== 'string' || typeof index !== 'number' || typeof to !== 'string') {
        return undefined;
    }

    return {
        graphId,
        index,
        transition: {
            condition: typeof condition === 'string' ? condition : undefined,
            from: selectedTransitionEdge.source,
            to,
        },
    };
}

export function isTextEntryElement(element: Element | null): boolean {
    if (!element) {
        return false;
    }

    if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA' || element.tagName === 'SELECT') {
        return true;
    }

    if ((element as HTMLElement).isContentEditable) {
        return true;
    }

    const contentEditableAttribute = element.getAttribute('contenteditable');

    return (
        contentEditableAttribute === '' ||
        contentEditableAttribute === 'true' ||
        contentEditableAttribute === 'plaintext-only'
    );
}

export const GRAPH_TRANSITION_EDITOR_ATTRIBUTE = 'data-graph-transition-editor';

const GRAPH_TRANSITION_EDITOR_SELECTOR = `[${GRAPH_TRANSITION_EDITOR_ATTRIBUTE}]`;

const LAYERED_SURFACE_SELECTOR = [
    '[role="dialog"]',
    '[role="alertdialog"]',
    '[role="listbox"]',
    '[role="menu"]',
    '[role="combobox"]',
    GRAPH_TRANSITION_EDITOR_SELECTOR,
].join(',');

export function isCanvasDeleteKeyTarget(element: Element | null): boolean {
    if (!element || element === document.body) {
        return !document.querySelector(GRAPH_TRANSITION_EDITOR_SELECTOR);
    }

    if (isTextEntryElement(element)) {
        return false;
    }

    return !element.closest(LAYERED_SURFACE_SELECTOR);
}
