import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import getDataPillsFromProperties from '@/pages/platform/workflow-editor/utils/getDataPillsFromProperties';
import getOutputSchemaFromWorkflowNodeOutput from '@/pages/platform/workflow-editor/utils/getOutputSchemaFromWorkflowNodeOutput';
import {useGetPreviousWorkflowNodeOutputsQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ComponentPropertiesType} from '@/shared/types';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/shallow';

export default function useAiAgentTestDataPills() {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const {componentDefinitions, setDataPills, taskDispatcherDefinitions, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            componentDefinitions: state.componentDefinitions,
            setDataPills: state.setDataPills,
            taskDispatcherDefinitions: state.taskDispatcherDefinitions,
            workflow: state.workflow,
        }))
    );

    const {data: workflowNodeOutputs} = useGetPreviousWorkflowNodeOutputsQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            lastWorkflowNodeName: rootClusterElementNodeData?.workflowNodeName,
        },
        !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    const calculatedDataPills = useMemo(() => {
        if (!workflowNodeOutputs?.length || !componentDefinitions?.length) {
            return [];
        }

        const definitionsMap = new Map(
            [...componentDefinitions, ...(taskDispatcherDefinitions ?? [])].map((definition) => [
                definition.name,
                definition,
            ])
        );

        const filteredOutputs = workflowNodeOutputs.filter((output) => {
            const {actionDefinition, taskDispatcherDefinition, triggerDefinition} = output;

            return actionDefinition || triggerDefinition || taskDispatcherDefinition?.name === 'loop';
        });

        const previousDefinitions = filteredOutputs
            .map((output) => {
                const componentName =
                    output.actionDefinition?.componentName ||
                    output.triggerDefinition?.componentName ||
                    (output.taskDispatcherDefinition?.name === 'loop' ? 'loop' : undefined);

                return componentName ? definitionsMap.get(componentName) : undefined;
            })
            .filter(Boolean);

        const filteredNodeNames = filteredOutputs.map((output) => output.workflowNodeName);

        const componentProperties: Array<ComponentPropertiesType> = previousDefinitions.map(
            (componentDefinition, index) => {
                const outputSchemaDefinition = getOutputSchemaFromWorkflowNodeOutput(filteredOutputs[index]);

                const properties = outputSchemaDefinition?.properties?.length
                    ? outputSchemaDefinition.properties
                    : outputSchemaDefinition?.items;

                return {
                    componentDefinition: componentDefinition!,
                    properties,
                    workflowNodeName: filteredOutputs[index]?.workflowNodeName,
                };
            }
        );

        return getDataPillsFromProperties(componentProperties, filteredNodeNames).flat(Infinity);
    }, [componentDefinitions, taskDispatcherDefinitions, workflowNodeOutputs]);

    useEffect(() => {
        if (calculatedDataPills.length > 0) {
            setDataPills(calculatedDataPills);
        }
    }, [calculatedDataPills, setDataPills]);
}
