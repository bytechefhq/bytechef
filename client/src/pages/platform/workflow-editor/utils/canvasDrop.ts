import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {CANVAS_DRAG_DATA_TYPE, getCanvasDragKind} from './canvasDragData';
import resolveCanvasDropTarget from './resolveCanvasDropTarget';
import resolveTargetTriggerName from './resolveTargetTriggerName';

type CanvasDragEventType = Pick<DragEvent, 'preventDefault' | 'target'> & {dataTransfer: DataTransfer};

interface CanvasDropHandlersI {
    onDropOnPlaceholderNode: (targetNode: Node, droppedNode: ClickedDefinitionType) => void;
    onDropOnTriggerNode: (droppedNode: ClickedDefinitionType, targetTriggerName: string) => void;
    onDropOnTriggerPlaceholder: (droppedNode: ClickedDefinitionType) => void;
    onDropOnWorkflowEdge: (targetEdge: Edge, droppedNode: ClickedDefinitionType) => void;
}

interface HandleCanvasDropProps {
    componentDefinitions: ComponentDefinitionBasic[];
    edges: Edge[];
    event: CanvasDragEventType;
    handlers: CanvasDropHandlersI;
    nodes: Node[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

export function handleCanvasDragOver(event: CanvasDragEventType, edges: Edge[], nodes: Node[]) {
    if (event.target instanceof HTMLButtonElement && event.target.dataset.nodeType === 'workflow') {
        return;
    }

    const dragKind = getCanvasDragKind(event.dataTransfer);

    if (dragKind && !resolveCanvasDropTarget({dragKind, edges, nodes, target: event.target})) {
        event.dataTransfer.dropEffect = 'none';

        return;
    }

    event.preventDefault();

    event.dataTransfer.dropEffect = 'move';
}

function findDroppedNode(
    droppedNodeName: string,
    componentDefinitions: ComponentDefinitionBasic[],
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[]
): ClickedDefinitionType | undefined {
    const componentDefinition = componentDefinitions.find((definition) => definition.name === droppedNodeName);

    if (componentDefinition) {
        return componentDefinition as ClickedDefinitionType;
    }

    const taskDispatcherDefinition = taskDispatcherDefinitions.find(
        (definition) => definition.name === droppedNodeName
    );

    return taskDispatcherDefinition
        ? ({...taskDispatcherDefinition, taskDispatcher: true} as ClickedDefinitionType)
        : undefined;
}

export function handleCanvasDrop({
    componentDefinitions,
    edges,
    event,
    handlers,
    nodes,
    taskDispatcherDefinitions,
}: HandleCanvasDropProps) {
    const [droppedNodeName, droppedNodeType] = event.dataTransfer.getData(CANVAS_DRAG_DATA_TYPE).split('--');

    const droppedNode = findDroppedNode(droppedNodeName, componentDefinitions, taskDispatcherDefinitions);

    if (!droppedNode) {
        return;
    }

    const isTriggerDrop = droppedNodeType === 'trigger';

    const dropTarget = resolveCanvasDropTarget({
        dragKind: isTriggerDrop ? 'trigger' : 'task',
        edges,
        nodes,
        target: event.target,
    });

    if (!dropTarget) {
        return;
    }

    if (dropTarget.type === 'triggerPlaceholder') {
        handlers.onDropOnTriggerPlaceholder({...droppedNode, trigger: true});
    } else if (dropTarget.type === 'trigger') {
        const targetTriggerName = resolveTargetTriggerName(dropTarget.node.data as NodeDataType);

        if (targetTriggerName) {
            handlers.onDropOnTriggerNode({...droppedNode, trigger: true}, targetTriggerName);
        }
    } else if (dropTarget.type === 'placeholder') {
        handlers.onDropOnPlaceholderNode(dropTarget.node, droppedNode);
    } else {
        handlers.onDropOnWorkflowEdge(dropTarget.edge, droppedNode);
    }
}
