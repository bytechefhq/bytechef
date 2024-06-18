import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetContent} from '@/components/ui/sheet';
import useProjectInstanceWorkflowSheetStore from '@/pages/automation/project-instances/stores/useProjectInstanceWorkflowSheetStore';
import defaultNodes from '@/pages/platform/workflow-editor/nodes/defaultNodes';
import WorkflowEdgeReadOnly from '@/shared/components/WorkflowEdgeReadOnly';
import WorkflowNodeReadOnly from '@/shared/components/WorkflowNodeReadOnly';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Component1Icon} from '@radix-ui/react-icons';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import ReactFlow, {Controls, Edge, MiniMap, Node} from 'reactflow';

const ProjectInstanceWorkflowSheet = () => {
    const {projectInstanceWorkflowSheetOpen, setProjectInstanceWorkflowSheetOpen, workflowId} =
        useProjectInstanceWorkflowSheetStore();

    const {data: workflow} = useGetWorkflowQuery(workflowId!, !!workflowId);

    const nodeTypes = useMemo(
        () => ({
            workflow: WorkflowNodeReadOnly,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            workflow: WorkflowEdgeReadOnly,
        }),
        []
    );

    const workflowComponentNames = [
        ...(workflow?.workflowTriggerComponentNames ?? []),
        ...(workflow?.workflowTaskComponentNames ?? []),
    ];

    const {data: componentDefinitions, isFetched: componentDefinitionsFetched} = useGetComponentDefinitionsQuery(
        {include: workflowComponentNames},
        workflowComponentNames !== undefined
    );

    const defaultNodesWithWorkflowNodes: Array<Node> | undefined = useMemo(() => {
        if (!workflow) {
            return defaultNodes;
        }

        const workflowTasks = workflow.tasks?.filter((task) => task.name);
        const workflowTrigger = workflow.triggers?.[0] || defaultNodes[0].data;

        let workflowComponents = workflowTasks;

        if (workflowTrigger) {
            workflowComponents = [workflowTrigger, ...(workflowTasks || [])];
        }

        const workflowNodes = workflowComponents?.map((component, index) => {
            const componentName = component.type?.split('/')[0];
            const operationName = component.type?.split('/')[2];

            let componentDefinition = componentDefinitions?.find(
                (componentDefinition) => componentDefinition.name === componentName
            );

            if (componentDefinition == undefined) {
                componentDefinition = componentDefinitions?.find(
                    (componentDefinition) => componentDefinition.name === 'missing'
                );
            }

            return {
                data: {
                    ...component,
                    componentName: componentDefinition?.name,
                    icon: (
                        <InlineSVG
                            className="size-9"
                            loader={<Component1Icon className="size-9 flex-none text-gray-900" />}
                            src={componentDefinition?.icon ?? 'https://via.placeholder.com/24'}
                        />
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
    }, [componentDefinitions, workflow?.id]);

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
    }, [defaultNodesWithWorkflowNodes, workflow, workflow?.id]);

    return (
        <Sheet onOpenChange={() => setProjectInstanceWorkflowSheetOpen(!projectInstanceWorkflowSheetOpen)} open>
            <SheetContent className="flex w-2/3 flex-col p-4 sm:max-w-screen-xl">
                <h1 className="text-lg font-semibold">
                    {workflow?.label} <span className="text-base font-normal text-gray-500">(read-only)</span>
                </h1>

                {componentDefinitionsFetched ? (
                    <div className="flex h-full flex-1 flex-col rounded-xl bg-muted">
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
                ) : (
                    <LoadingIcon />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default ProjectInstanceWorkflowSheet;
