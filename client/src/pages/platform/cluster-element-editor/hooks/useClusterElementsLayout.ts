import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {ComponentDefinition, ComponentDefinitionApi} from '@/shared/middleware/platform/configuration';
import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {Edge, Node} from '@xyflow/react';
import {useEffect, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../../workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getLayoutedElements} from '../../workflow-editor/utils/layoutUtils';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import createClusterElementsEdges from '../utils/createClusterElementsEdges';
import createClusterElementsNodes from '../utils/createClusterElementsNodes';

const useClusterElementsLayout = () => {
    const [nestedClusterRootsDefinitions, setNestedClusterRootsDefinitions] = useState<
        Record<string, ComponentDefinition>
    >({});

    const {rootClusterElementNodeData} = useWorkflowEditorStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();
    const {workflow} = useWorkflowDataStore.getState();

    const queryClient = useQueryClient();

    const rootClusterElementComponentVersion =
        Number(rootClusterElementNodeData?.type?.split('/')[1].replace(/^v/, '')) || 1;

    const rootClusterElementComponentName = rootClusterElementNodeData?.componentName || '';

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: rootClusterElementComponentName,
            componentVersion: rootClusterElementComponentVersion,
        },
        !!rootClusterElementNodeData && currentNode?.rootClusterElement
    );

    const {nodes, setEdges, setNodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    const nodePositions = nodes.reduce<Record<string, {x: number; y: number}>>((accumulator, node) => {
        accumulator[node.id] = {
            x: node.position.x,
            y: node.position.y,
        };
        return accumulator;
    }, {});

    const canvasWidth = window.innerWidth - 80;

    const {allNodes, taskEdges} = useMemo(() => {
        const nodes: Array<Node> = [];
        const edges: Array<Edge> = [];

        if (!rootClusterElementNodeData || !rootClusterElementDefinition || !workflow.definition) {
            return {allNodes: nodes, taskEdges: edges};
        }

        const rootClusterElementNode = {
            data: rootClusterElementNodeData,
            id: rootClusterElementNodeData.workflowNodeName,
            position:
                rootClusterElementNodeData.metadata?.ui?.nodePosition ||
                nodePositions[rootClusterElementNodeData.workflowNodeName] ||
                DEFAULT_NODE_POSITION,
            type: 'workflow',
        };

        nodes.push(rootClusterElementNode);

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

        const currentClusterRootTask = workflowDefinitionTasks.find(
            (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
        );

        const clusterElements = currentClusterRootTask.clusterElements || {};

        const childNodes = createClusterElementsNodes({
            clusterElements,
            clusterRootComponentDefinition: rootClusterElementDefinition,
            clusterRootId: rootClusterElementNodeData.workflowNodeName,
            currentNodePositions: nodePositions,
            nestedClusterRootsDefinitions: nestedClusterRootsDefinitions || {},
        });

        nodes.push(...childNodes);

        const childEdges = createClusterElementsEdges({
            clusterRootComponentDefinition: rootClusterElementDefinition,
            clusterRootId: rootClusterElementNodeData.workflowNodeName,
            nestedClusterRootsDefinitions: nestedClusterRootsDefinitions || {},
            nodes,
        });

        edges.push(...childEdges);

        return {allNodes: nodes, taskEdges: edges};

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [nestedClusterRootsDefinitions, rootClusterElementNodeData, rootClusterElementDefinition, workflow]);

    useEffect(() => {
        if (!rootClusterElementNodeData || !rootClusterElementDefinition || !workflow.definition) {
            return;
        }

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

        const currentClusterRootTask = workflowDefinitionTasks.find(
            (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
        );
        const clusterElements = currentClusterRootTask.clusterElements || {};

        const clusterRoots: {componentName: string; componentVersion: number}[] = [];

        const findClusterRoots = (elements: ClusterElementsType) => {
            Object.entries(elements).forEach(([, value]) => {
                if (Array.isArray(value)) {
                    (value as ClusterElementItemType[]).forEach((item) => {
                        if (item.clusterElements) {
                            clusterRoots.push({
                                componentName: item.type.split('/')[0],
                                componentVersion: Number(item.type?.split('/')[1]?.replace(/^v/, '')) || 1,
                            });

                            findClusterRoots(item.clusterElements);
                        }
                    });
                } else if (value && typeof value === 'object') {
                    const typedValue = value as ClusterElementItemType;

                    if (typedValue.clusterElements) {
                        clusterRoots.push({
                            componentName: typedValue.type.split('/')[0],
                            componentVersion: Number(typedValue.type?.split('/')[1]?.replace(/^v/, '')) || 1,
                        });

                        findClusterRoots(typedValue.clusterElements);
                    }
                }
            });
        };

        findClusterRoots(clusterElements);

        const fetchDefinitions = async () => {
            const definitions: Record<string, ComponentDefinition> = {};

            await Promise.all(
                clusterRoots.map(async (root) => {
                    const definition = await queryClient.fetchQuery({
                        queryFn: () =>
                            new ComponentDefinitionApi().getComponentDefinition({
                                componentName: root.componentName,
                                componentVersion: root.componentVersion,
                            }),
                        queryKey: ComponentDefinitionKeys.componentDefinition({
                            componentName: root.componentName,
                            componentVersion: root.componentVersion,
                        }),
                    });

                    definitions[root.componentName] = definition;
                })
            );

            setNestedClusterRootsDefinitions(definitions);
        };

        fetchDefinitions();
    }, [rootClusterElementNodeData, rootClusterElementDefinition, workflow, queryClient]);

    useEffect(() => {
        const layoutNodes = allNodes;
        const edges: Edge[] = taskEdges;

        const elements = getLayoutedElements({canvasWidth, edges, isClusterElementsCanvas: true, nodes: layoutNodes});

        setNodes(elements.nodes);
        setEdges(elements.edges);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, rootClusterElementNodeData, allNodes]);
};

export default useClusterElementsLayout;
