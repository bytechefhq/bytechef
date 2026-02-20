import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';

interface FilterWorkflowNodeOutputsResultI {
    definitions: ComponentDefinitionBasic[];
    outputs: WorkflowNodeOutput[];
}

export default function filterWorkflowNodeOutputs(
    workflowNodeOutputs: WorkflowNodeOutput[],
    componentDefinitions: ComponentDefinitionBasic[],
    taskDispatcherDefinitions: ComponentDefinitionBasic[]
): FilterWorkflowNodeOutputsResultI {
    const definitionsMap = new Map(
        [...componentDefinitions, ...taskDispatcherDefinitions].map((definition) => [definition.name, definition])
    );

    return workflowNodeOutputs.reduce(
        (acc, output) => {
            const {actionDefinition, taskDispatcherDefinition, triggerDefinition} = output;

            if (
                !actionDefinition &&
                !triggerDefinition &&
                !taskDispatcherDefinition?.outputDefined &&
                !taskDispatcherDefinition?.variablePropertiesDefined
            ) {
                return acc;
            }

            let componentName: string | undefined;

            if (actionDefinition?.componentName) {
                componentName = actionDefinition.componentName;
            } else if (triggerDefinition?.componentName) {
                componentName = triggerDefinition.componentName;
            } else if (taskDispatcherDefinition?.name) {
                componentName = taskDispatcherDefinition.name;
            }

            const matchingDefinition = componentName ? definitionsMap.get(componentName) : undefined;

            if (matchingDefinition) {
                acc.definitions.push(matchingDefinition);

                acc.outputs.push(output);
            }

            return acc;
        },
        {definitions: [] as ComponentDefinitionBasic[], outputs: [] as WorkflowNodeOutput[]}
    );
}
