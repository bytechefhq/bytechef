import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {Edge, Node} from '@xyflow/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {CANVAS_DRAG_DATA_TYPE, TRIGGER_DRAG_DATA_TYPE, getClusterElementTypeDragDataType} from '../canvasDragData';
import {handleCanvasDragOver, handleCanvasDrop} from '../canvasDrop';

const nodes: Node[] = [
    {data: {trigger: true, workflowNodeName: 'trigger_1'}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'},
    {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 300, y: 0}, type: 'triggerPlaceholder'},
    {data: {label: '+'}, id: 'final-placeholder', position: {x: 0, y: 400}, type: 'placeholder'},
];

const edges: Edge[] = [{id: 'trigger_1=>logger_1', source: 'trigger_1', target: 'logger_1', type: 'workflow'}];

const componentDefinitions = [{name: 'logger'}, {name: 'schedule'}] as ComponentDefinitionBasic[];

const taskDispatcherDefinitions = [{name: 'condition'}] as TaskDispatcherDefinitionBasic[];

const renderNode = (nodeId: string) => {
    const nodeElement = document.createElement('div');

    nodeElement.className = 'react-flow__node';
    nodeElement.dataset.id = nodeId;
    nodeElement.innerHTML = '<div><svg><path></path></svg></div>';

    document.body.appendChild(nodeElement);

    return nodeElement.querySelector('path')!;
};

const renderEdgeDropzone = (edgeId: string) => {
    const edgeElement = document.createElement('div');

    edgeElement.id = edgeId;
    edgeElement.innerHTML = `<div id="${edgeId}-button"><svg></svg></div>`;

    document.body.appendChild(edgeElement);

    return edgeElement.querySelector('svg')!;
};

const createDragEvent = (target: EventTarget, data: string, types: string[]) => {
    const dataTransfer = {dropEffect: 'none', getData: vi.fn(() => data), types} as unknown as DataTransfer;

    return {dataTransfer, preventDefault: vi.fn(), target};
};

const createHandlers = () => ({
    onDropOnClusterElementPlaceholder: vi.fn(),
    onDropOnPlaceholderNode: vi.fn(),
    onDropOnTriggerNode: vi.fn(),
    onDropOnTriggerPlaceholder: vi.fn(),
    onDropOnWorkflowEdge: vi.fn(),
});

const drop = (event: ReturnType<typeof createDragEvent>, handlers: ReturnType<typeof createHandlers>) =>
    handleCanvasDrop({componentDefinitions, edges, event, handlers, nodes, taskDispatcherDefinitions});

describe('handleCanvasDragOver', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('allows a task drag over a placeholder', () => {
        const event = createDragEvent(renderNode('final-placeholder'), '', [CANVAS_DRAG_DATA_TYPE]);

        handleCanvasDragOver(event, edges, nodes);

        expect(event.preventDefault).toHaveBeenCalled();
        expect(event.dataTransfer.dropEffect).toBe('move');
    });

    it('refuses a trigger drag over a placeholder', () => {
        const event = createDragEvent(renderNode('final-placeholder'), '', [
            CANVAS_DRAG_DATA_TYPE,
            TRIGGER_DRAG_DATA_TYPE,
        ]);

        handleCanvasDragOver(event, edges, nodes);

        expect(event.preventDefault).not.toHaveBeenCalled();
        expect(event.dataTransfer.dropEffect).toBe('none');
    });

    it('leaves drags that did not start in the components panel alone', () => {
        const event = createDragEvent(document.body, '', ['text/plain']);

        handleCanvasDragOver(event, edges, nodes);

        expect(event.preventDefault).toHaveBeenCalled();
        expect(event.dataTransfer.dropEffect).toBe('move');
    });

    it('ignores drags over a workflow node button', () => {
        const button = document.createElement('button');

        button.dataset.nodeType = 'workflow';

        const event = createDragEvent(button, '', [CANVAS_DRAG_DATA_TYPE]);

        handleCanvasDragOver(event, edges, nodes);

        expect(event.preventDefault).not.toHaveBeenCalled();
    });
});

describe('handleCanvasDrop', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('drops a task on the placeholder under the plus icon', () => {
        const handlers = createHandlers();

        drop(createDragEvent(renderNode('final-placeholder'), 'logger', [CANVAS_DRAG_DATA_TYPE]), handlers);

        expect(handlers.onDropOnPlaceholderNode).toHaveBeenCalledWith(nodes[2], {name: 'logger'});
    });

    it('drops a task dispatcher on an edge', () => {
        const handlers = createHandlers();

        drop(
            createDragEvent(renderEdgeDropzone('trigger_1=>logger_1'), "condition--taskDispatcher'", [
                CANVAS_DRAG_DATA_TYPE,
            ]),
            handlers
        );

        expect(handlers.onDropOnWorkflowEdge).toHaveBeenCalledWith(edges[0], {
            name: 'condition',
            taskDispatcher: true,
        });
    });

    it('adds a trigger dropped on the trigger slot', () => {
        const handlers = createHandlers();

        drop(createDragEvent(renderNode(TRIGGER_PLACEHOLDER_NODE_ID), 'schedule--trigger', []), handlers);

        expect(handlers.onDropOnTriggerPlaceholder).toHaveBeenCalledWith({name: 'schedule', trigger: true});
    });

    it('replaces the trigger a trigger is dropped on', () => {
        const handlers = createHandlers();

        drop(createDragEvent(renderNode('trigger_1'), 'schedule--trigger', []), handlers);

        expect(handlers.onDropOnTriggerNode).toHaveBeenCalledWith({name: 'schedule', trigger: true}, 'trigger_1');
    });

    it('ignores a trigger dropped on a placeholder and an unknown component', () => {
        const handlers = createHandlers();

        drop(createDragEvent(renderNode('final-placeholder'), 'schedule--trigger', []), handlers);
        drop(createDragEvent(renderNode('final-placeholder'), 'unknown', []), handlers);

        for (const handler of Object.values(handlers)) {
            expect(handler).not.toHaveBeenCalled();
        }
    });

    it('ignores a trigger dropped on a trigger without a name', () => {
        const handlers = createHandlers();

        handleCanvasDrop({
            componentDefinitions,
            edges,
            event: createDragEvent(renderNode('trigger_1'), 'schedule--trigger', []),
            handlers,
            nodes: [{...nodes[0], data: {trigger: true}}],
            taskDispatcherDefinitions,
        });

        expect(handlers.onDropOnTriggerNode).not.toHaveBeenCalled();
    });
});

describe('handleCanvasDrop inside an inline cluster frame', () => {
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

    const clusterComponentDefinitions = [
        {clusterElementsCount: {MODEL: 1}, name: 'openAi'},
        {name: 'logger'},
    ] as ComponentDefinitionBasic[];

    const dropInFrame = (event: ReturnType<typeof createDragEvent>, handlers: ReturnType<typeof createHandlers>) =>
        handleCanvasDrop({
            componentDefinitions: clusterComponentDefinitions,
            edges: [...edges, clusterElementEdge],
            event,
            handlers,
            nodes: [...nodes, clusterElementPlaceholderNode],
            taskDispatcherDefinitions,
        });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('adds a component dropped on a cluster element placeholder to that cluster root, not to the main flow', () => {
        const handlers = createHandlers();

        dropInFrame(
            createDragEvent(renderNode(clusterElementPlaceholderNode.id), 'openAi--task', [CANVAS_DRAG_DATA_TYPE]),
            handlers
        );

        expect(handlers.onDropOnClusterElementPlaceholder).toHaveBeenCalledWith(
            clusterElementPlaceholderNode,
            expect.objectContaining({name: 'openAi'})
        );
        expect(handlers.onDropOnPlaceholderNode).not.toHaveBeenCalled();
    });

    it('ignores a component without an element of the placeholder type', () => {
        const handlers = createHandlers();

        dropInFrame(
            createDragEvent(renderNode(clusterElementPlaceholderNode.id), 'logger--task', [CANVAS_DRAG_DATA_TYPE]),
            handlers
        );

        expect(Object.values(handlers).every((handler) => handler.mock.calls.length === 0)).toBe(true);
    });

    it('allows a drag over a cluster element placeholder when the component has an element of its type', () => {
        const event = createDragEvent(renderNode(clusterElementPlaceholderNode.id), '', [
            CANVAS_DRAG_DATA_TYPE,
            getClusterElementTypeDragDataType('MODEL'),
        ]);

        handleCanvasDragOver(event, edges, [...nodes, clusterElementPlaceholderNode]);

        expect(event.preventDefault).toHaveBeenCalled();
        expect(event.dataTransfer.dropEffect).toBe('move');
    });

    it('refuses a drag over a cluster element placeholder when the component has no element of its type', () => {
        const event = createDragEvent(renderNode(clusterElementPlaceholderNode.id), '', [
            CANVAS_DRAG_DATA_TYPE,
            getClusterElementTypeDragDataType('TOOLS'),
        ]);

        handleCanvasDragOver(event, edges, [...nodes, clusterElementPlaceholderNode]);

        expect(event.preventDefault).not.toHaveBeenCalled();
        expect(event.dataTransfer.dropEffect).toBe('none');
    });

    it('refuses a drag over an edge between a cluster root and its elements', () => {
        const event = createDragEvent(renderEdgeDropzone(clusterElementEdge.id), '', [CANVAS_DRAG_DATA_TYPE]);

        handleCanvasDragOver(event, [...edges, clusterElementEdge], nodes);

        expect(event.preventDefault).not.toHaveBeenCalled();
        expect(event.dataTransfer.dropEffect).toBe('none');
    });
});
