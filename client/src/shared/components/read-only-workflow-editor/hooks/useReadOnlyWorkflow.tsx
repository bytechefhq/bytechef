import defaultNodes from '@/shared/defaultNodes';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';

import useReadOnlyWorkflowStore from '../stores/useReadOnlyWorkflowStore';

const useReadOnlyWorkflow = () => {
    const {isReadOnlyWorkflowSheetOpen, setIsReadOnlyWorkflowSheetOpen, setWorkflow, workflow} =
        useReadOnlyWorkflowStore();

    const workflowComponentNames = useMemo(
        () => [...(workflow?.workflowTriggerComponentNames ?? []), ...(workflow?.workflowTaskComponentNames ?? [])],
        [workflow?.workflowTriggerComponentNames, workflow?.workflowTaskComponentNames]
    );

    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery(
        {include: workflowComponentNames},
        workflowComponentNames !== undefined
    );

    const nodes: Node[] = useMemo(() => {
        if (!workflow || !componentDefinitions) {
            return defaultNodes;
        }

        const workflowTasks = workflow.tasks?.filter((task) => task.name);
        const workflowTrigger = workflow.triggers?.[0] || defaultNodes[0].data;

        let workflowComponents: Array<{name: string; type: string}> | undefined = workflowTasks;

        if (!workflowComponents?.length) {
            return defaultNodes;
        }

        if (workflowTrigger) {
            workflowComponents = [workflowTrigger as {name: string; type: string}, ...(workflowTasks || [])];
        }

        return workflowComponents?.map((component, index) => {
            const componentName = component.type?.split('/')[0];
            const operationName = component.type?.split('/')[2];

            let componentDefinition = componentDefinitions?.find((componentDef) => componentDef.name === componentName);

            if (!componentDefinition) {
                componentDefinition = componentDefinitions?.find((componentDef) => componentDef.name === 'missing');
            }

            return {
                data: {
                    ...component,
                    componentName: componentDefinition?.name,
                    icon: componentDefinition?.icon ? (
                        <InlineSVG
                            className="size-9"
                            loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                            src={componentDefinition?.icon}
                        />
                    ) : (
                        <ComponentIcon className="size-9 flex-none text-gray-900" />
                    ),
                    id: componentDefinition?.name,
                    label: componentDefinition?.title,
                    name: component.name,
                    operationName,
                    trigger: index === 0,
                    type: 'workflow',
                },
                id: component.name,
                position: {x: 0, y: 150 * index},
                type: 'readonly',
            };
        });
    }, [componentDefinitions, workflow]);

    const edges = useMemo(() => {
        const edges: Array<Edge> = [];

        if (nodes && nodes.length > 1) {
            nodes.forEach((node, index) => {
                const nextNode = nodes[index + 1];

                if (nextNode) {
                    edges.push({
                        id: `${node.id}=>${nextNode.id}`,
                        source: node.id,
                        target: nextNode.id,
                        type: 'readonly',
                    });
                }
            });
        }

        return edges;
    }, [nodes]);

    function closeReadOnlyWorkflowSheet() {
        setIsReadOnlyWorkflowSheetOpen(false);
    }

    function openReadOnlyWorkflowSheet(workflow: Workflow) {
        if (!workflow) {
            return;
        }

        setWorkflow(workflow);

        setIsReadOnlyWorkflowSheetOpen(true);
    }

    const isLoading = isComponentDefinitionsLoading || !componentDefinitions || !workflow;

    return {
        closeReadOnlyWorkflowSheet,
        edges,
        isLoading,
        isReadOnlyWorkflowSheetOpen,
        nodes,
        openReadOnlyWorkflowSheet,
        workflow,
    };
};

export default useReadOnlyWorkflow;
