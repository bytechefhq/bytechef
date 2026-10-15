import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {afterEach, describe, expect, it} from 'vitest';

import resolveCanvasDropTarget from '../resolveCanvasDropTarget';

const nodes: Node[] = [
    {data: {trigger: true}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'},
    {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 300, y: 0}, type: 'triggerPlaceholder'},
    {data: {}, id: 'logger_1', position: {x: 0, y: 200}, type: 'workflow'},
    {data: {label: '+'}, id: 'final-placeholder', position: {x: 0, y: 400}, type: 'placeholder'},
];

const edges: Edge[] = [{id: 'trigger_1=>logger_1', source: 'trigger_1', target: 'logger_1', type: 'workflow'}];

const renderNode = (nodeId: string, innerHtml: string) => {
    const nodeElement = document.createElement('div');

    nodeElement.className = 'react-flow__node';
    nodeElement.dataset.id = nodeId;
    nodeElement.innerHTML = innerHtml;

    document.body.appendChild(nodeElement);

    return nodeElement;
};

const renderEdgeDropzone = (edgeId: string) => {
    const edgeElement = document.createElement('div');

    edgeElement.id = edgeId;
    edgeElement.innerHTML = `<div><div id="${edgeId}-button"><svg><path></path></svg></div></div>`;

    document.body.appendChild(edgeElement);

    return edgeElement;
};

describe('resolveCanvasDropTarget', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('resolves a task dropped on the plus icon of a placeholder to that placeholder', () => {
        const nodeElement = renderNode('final-placeholder', '<div><svg><path></path></svg></div>');

        const target = nodeElement.querySelector('path');

        expect(resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target})).toEqual({
            node: nodes[3],
            type: 'placeholder',
        });
    });

    it('resolves nothing for a target that is not an element', () => {
        expect(resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target: null})).toBeUndefined();
        expect(resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target: document})).toBeUndefined();
    });

    it('resolves a task dropped on the placeholder box to that placeholder', () => {
        const nodeElement = renderNode('final-placeholder', '<div></div>');

        const target = nodeElement.firstElementChild;

        expect(resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target})?.type).toBe('placeholder');
    });

    it('resolves a task dropped on the plus icon of an edge to that edge', () => {
        const edgeElement = renderEdgeDropzone('trigger_1=>logger_1');

        const target = edgeElement.querySelector('path');

        expect(resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target})).toEqual({
            edge: edges[0],
            type: 'edge',
        });
    });

    it('rejects a task dropped on a trigger, the trigger slot or the empty canvas', () => {
        const triggerElement = renderNode('trigger_1', '<div></div>');
        const triggerPlaceholderElement = renderNode(TRIGGER_PLACEHOLDER_NODE_ID, '<div></div>');

        expect(resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target: triggerElement})).toBeUndefined();
        expect(
            resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target: triggerPlaceholderElement})
        ).toBeUndefined();
        expect(resolveCanvasDropTarget({dragKind: 'task', edges, nodes, target: document.body})).toBeUndefined();
    });

    it('resolves a trigger dropped on the trigger slot or an existing trigger', () => {
        const triggerPlaceholderElement = renderNode(TRIGGER_PLACEHOLDER_NODE_ID, '<div><svg></svg></div>');
        const triggerElement = renderNode('trigger_1', '<div></div>');

        expect(
            resolveCanvasDropTarget({
                dragKind: 'trigger',
                edges,
                nodes,
                target: triggerPlaceholderElement.querySelector('svg'),
            })
        ).toEqual({type: 'triggerPlaceholder'});
        expect(
            resolveCanvasDropTarget({
                dragKind: 'trigger',
                edges,
                nodes,
                target: triggerElement.firstElementChild,
            })
        ).toEqual({node: nodes[0], type: 'trigger'});
    });

    it('rejects a trigger dropped on a placeholder, an edge, a task or the empty canvas', () => {
        const placeholderElement = renderNode('final-placeholder', '<div></div>');
        const taskElement = renderNode('logger_1', '<div></div>');
        const edgeElement = renderEdgeDropzone('trigger_1=>logger_1');

        for (const target of [placeholderElement, taskElement, edgeElement, document.body]) {
            expect(resolveCanvasDropTarget({dragKind: 'trigger', edges, nodes, target})).toBeUndefined();
        }
    });
});

describe('resolveCanvasDropTarget inside an inline cluster frame', () => {
    const clusterElementPlaceholderNode: Node = {
        data: {clusterElementLabel: 'Model', clusterElementType: 'model', label: '+'},
        id: 'aiAgent_1-model-placeholder-0',
        parentId: 'aiAgent_1',
        position: {x: 120, y: 140},
        type: 'placeholder',
    };

    const clusterElementEdge: Edge = {
        id: 'aiAgent_1=>aiAgent_1-model-placeholder-0',
        source: 'aiAgent_1',
        target: 'aiAgent_1-model-placeholder-0',
        type: 'labeledClusterElementsEdge',
    };

    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('resolves a task dropped on a cluster element placeholder to that cluster element slot', () => {
        const nodeElement = renderNode(clusterElementPlaceholderNode.id, '<div><svg><path></path></svg></div>');

        expect(
            resolveCanvasDropTarget({
                dragKind: 'task',
                edges,
                nodes: [...nodes, clusterElementPlaceholderNode],
                target: nodeElement.querySelector('path'),
            })
        ).toEqual({node: clusterElementPlaceholderNode, type: 'clusterElementPlaceholder'});
    });

    it('resolves nothing for a task dropped on an edge between a cluster root and its elements', () => {
        const edgeElement = renderEdgeDropzone(clusterElementEdge.id);

        expect(
            resolveCanvasDropTarget({
                dragKind: 'task',
                edges: [...edges, clusterElementEdge],
                nodes,
                target: edgeElement.querySelector('path'),
            })
        ).toBeUndefined();
    });
});
