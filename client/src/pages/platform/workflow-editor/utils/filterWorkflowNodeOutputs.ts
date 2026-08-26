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
            const {
                actionDefinition,
                outputResponse,
                taskDispatcherDefinition,
                triggerDefinition,
                variableOutputResponse,
            } = output;

            // A task dispatcher is kept only when THIS response actually carries a schema, not merely because the
            // dispatcher type declares an output function. The server asks an ENCLOSING dispatcher for neither its
            // output nor its variable properties (`taskDispatcherOutput = false` in `WorkflowNodeOutputFacadeImpl`),
            // because a container has produced nothing while one of its own children is still being edited — so it
            // answers with both responses null. Loop and each stay visible through their variable properties
            // (`item`, `index`), which is exactly the distinction; condition, branch, fork-join and graph have none
            // and would otherwise render an empty section named after the container the node sits inside.
            if (!actionDefinition && !triggerDefinition) {
                if (!outputResponse?.outputSchema && !variableOutputResponse?.outputSchema) {
                    return acc;
                }
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
