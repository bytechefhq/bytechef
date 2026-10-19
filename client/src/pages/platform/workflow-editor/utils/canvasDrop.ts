import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {convertNameToSnakeCase} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {CANVAS_DRAG_DATA_TYPE, carriesClusterElementType, getCanvasDragKind} from './canvasDragData';
import resolveCanvasDropTarget from './resolveCanvasDropTarget';
import resolveTargetTriggerName from './resolveTargetTriggerName';

type CanvasDragEventType = Pick<DragEvent, 'preventDefault' | 'target'> & {dataTransfer: DataTransfer};

interface CanvasDropHandlersI {
    onDropOnClusterElementPlaceholder: (targetNode: Node, droppedNode: ClickedDefinitionType) => void;
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

    const dropTarget = dragKind ? resolveCanvasDropTarget({dragKind, edges, nodes, target: event.target}) : undefined;

    const isRefusedClusterElementDrop =
        dropTarget?.type === 'clusterElementPlaceholder' &&
        !carriesClusterElementType(event.dataTransfer, (dropTarget.node.data as NodeDataType).clusterElementType!);

    if (dragKind && (!dropTarget || isRefusedClusterElementDrop)) {
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
    } else if (dropTarget.type === 'clusterElementPlaceholder') {
        const clusterElementType = convertNameToSnakeCase((dropTarget.node.data as NodeDataType).clusterElementType!);

        const clusterElementsCount = componentDefinitions.find(
            (componentDefinition) => componentDefinition.name === droppedNodeName
        )?.clusterElementsCount;

        if (clusterElementsCount?.[clusterElementType]) {
            handlers.onDropOnClusterElementPlaceholder(dropTarget.node, droppedNode);
        }
    } else if (dropTarget.type === 'placeholder') {
        handlers.onDropOnPlaceholderNode(dropTarget.node, droppedNode);
    } else {
        handlers.onDropOnWorkflowEdge(dropTarget.edge, droppedNode);
    }
}
