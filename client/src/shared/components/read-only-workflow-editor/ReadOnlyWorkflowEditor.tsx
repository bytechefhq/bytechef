import defaultNodes from '@/shared/defaultNodes';
import {Controls, Edge, MiniMap, Node, ReactFlow} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';

import {ComponentDefinitionBasic, Workflow} from '../../middleware/platform/configuration';
import ReadOnlyEdge from './ReadOnlyEdge';
import ReadOnlyNode from './ReadOnlyNode';

const ReadOnlyWorkflowEditor = ({
    componentDefinitions,
    workflow,
}: {
    componentDefinitions: Array<ComponentDefinitionBasic>;
    workflow: Workflow;
}) => {
    const nodeTypes = useMemo(
        () => ({
            workflow: ReadOnlyNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            workflow: ReadOnlyEdge,
        }),
        []
    );

    const defaultNodesWithWorkflowNodes: Array<Node> | undefined = useMemo(() => {
        if (!workflow) {
            return defaultNodes;
        }

        const workflowTasks = workflow.tasks?.filter((task) => task.name);
        const workflowTrigger = workflow.triggers?.[0] || defaultNodes[0].data;

        let workflowComponents: Array<{name: string; type: string}> | undefined = workflowTasks;

        if (workflowTrigger) {
            workflowComponents = [workflowTrigger as {name: string; type: string}, ...(workflowTasks || [])];
        }

        const workflowNodes = workflowComponents?.map((component, index) => {
            const componentName = component.type?.split('/')[0];
            const operationName = component.type?.split('/')[2];

            let componentDefinition = componentDefinitions?.find(
                (componentDefinition) => componentDefinition.name === componentName
            );

            if (!componentDefinition) {
                componentDefinition = componentDefinitions?.find(
                    (componentDefinition) => componentDefinition.name === 'missing'
                );
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
                type: 'workflow',
            };
        });

        if (workflowNodes?.length) {
            return workflowNodes;
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentDefinitions, workflow.id]);

    const defaultEdgesWithWorkflowEdges = useMemo(() => {
        const workflowEdges: Array<Edge> = [];

        if (defaultNodesWithWorkflowNodes) {
            defaultNodesWithWorkflowNodes.forEach((node, index) => {
                const nextNode = defaultNodesWithWorkflowNodes[index + 1];

                if (nextNode) {
                    workflowEdges.push({
                        id: `${node!.id}=>${nextNode?.id}`,
                        source: node!.id,
                        target: nextNode?.id,
                        type: 'workflow',
                    });
                }
            });

            return workflowEdges;
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [defaultNodesWithWorkflowNodes, workflow.id]);

    return (
        <div className="flex h-full flex-1 flex-col rounded-xl bg-transparent">
            <ReactFlow
                defaultEdges={defaultEdgesWithWorkflowEdges}
                defaultNodes={defaultNodesWithWorkflowNodes || defaultNodes}
                deleteKeyCode={null}
                edgeTypes={edgeTypes}
                fitView
                fitViewOptions={{
                    maxZoom: 1,
                }}
                maxZoom={1.5}
                minZoom={0.6}
                nodeTypes={nodeTypes}
                nodesConnectable={false}
                nodesDraggable={false}
                panOnDrag
                panOnScroll
                proOptions={{hideAttribution: true}}
                zoomOnDoubleClick={false}
                zoomOnScroll={false}
            >
                <MiniMap />

                <Controls />
            </ReactFlow>
        </div>
    );
};

export default ReadOnlyWorkflowEditor;
