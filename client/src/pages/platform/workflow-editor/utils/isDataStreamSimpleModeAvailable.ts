import {getTask} from './getTask';

export function isDataStreamSimpleModeAvailable(
    workflowDefinition: string | undefined,
    workflowNodeName: string | undefined
): boolean {
    if (!workflowNodeName || !workflowDefinition) {
        return true;
    }

    let definition;

    try {
        definition = JSON.parse(workflowDefinition);
    } catch {
        return true;
    }

    const rootTask = getTask({tasks: definition.tasks ?? [], workflowNodeName});

    if (!rootTask?.clusterElements) {
        return true;
    }

    const processorValue = rootTask.clusterElements['processor'];

    if (!processorValue) {
        return true;
    }

    const processorElement = Array.isArray(processorValue) ? processorValue[0] : processorValue;

    const typeSegments = processorElement?.type?.split('/') ?? [];
    const componentName = typeSegments[0] ?? '';
    const operationName = typeSegments[2] ?? '';

    return componentName === 'dataStreamProcessor' && operationName === 'fieldMapper';
}
