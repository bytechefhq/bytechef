import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {ComponentDefinitionApi} from '@/shared/middleware/platform/configuration';
import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClusterElementItemType, ClusterElementsType, NestedClusterRootComponentDefinitionType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {Edge, Node} from '@xyflow/react';
import {useCallback, useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useDataPillPanelStore from '../../workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '../../workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getLayoutedElements} from '../../workflow-editor/utils/layoutUtils';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import {isPlainObject} from '../utils/clusterElementsUtils';
import createClusterElementsEdges from '../utils/createClusterElementsEdges';
import createClusterElementsNodes from '../utils/createClusterElementsNodes';

const useClusterElementsLayout = () => {
    const {
        mainClusterRootComponentDefinition,
        nestedClusterRootsComponentDefinitions,
        rootClusterElementNodeData,
        setMainClusterRootComponentDefinition,
        setNestedClusterRootsComponentDefinitions,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            mainClusterRootComponentDefinition: state.mainClusterRootComponentDefinition,
            nestedClusterRootsComponentDefinitions: state.nestedClusterRootsComponentDefinitions,
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setMainClusterRootComponentDefinition: state.setMainClusterRootComponentDefinition,
            setNestedClusterRootsComponentDefinitions: state.setNestedClusterRootsComponentDefinitions,
        }))
    );
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );
    const {isNodeDragging, isPositionSaving} = useClusterElementsDataStore(
        useShallow((state) => ({
            isNodeDragging: state.isNodeDragging,
            isPositionSaving: state.isPositionSaving,
        }))
    );
    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
        }))
    );
    const {dataPillPanelOpen} = useDataPillPanelStore(
        useShallow((state) => ({
            dataPillPanelOpen: state.dataPillPanelOpen,
        }))
    );

    const queryClient = useQueryClient();

    const mainClusterRootQueryParameters = useMemo(() => {
        if (!rootClusterElementNodeData?.type || !rootClusterElementNodeData?.componentName) {
            return {
                componentName: '',
                componentVersion: 1,
            };
        }

        return {
            componentName: rootClusterElementNodeData?.componentName,
            componentVersion: Number(rootClusterElementNodeData?.type?.split('/')[1]?.replace(/^v/, '')) || 1,
        };
    }, [rootClusterElementNodeData]);

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        mainClusterRootQueryParameters,
        !!rootClusterElementNodeData?.workflowNodeName
    );

    const {setEdges, setNodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    let canvasWidth = window.innerWidth;

    if (workflowNodeDetailsPanelOpen) {
        canvasWidth -= 460;

        if (dataPillPanelOpen) {
            canvasWidth -= 400;
        }
    } else {
        canvasWidth -= 80;
    }

    const workflowDefinitionTasks = useMemo(() => {
        if (!workflow.definition) {
            return [];
        }

        return JSON.parse(workflow.definition).tasks;
    }, [workflow.definition]);

    const mainRootClusterElementTask = useMemo(
        () =>
            workflowDefinitionTasks.find(
                (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
            ),
        [workflowDefinitionTasks, rootClusterElementNodeData?.workflowNodeName]
    );

    const clusterElements = useMemo(
        () => mainRootClusterElementTask?.clusterElements || {},
        [mainRootClusterElementTask?.clusterElements]
    );

    const {allNodes, taskEdges} = useMemo(() => {
        const nodes: Array<Node> = [];
        const edges: Array<Edge> = [];

        if (!rootClusterElementNodeData || !mainClusterRootComponentDefinition || !workflow.definition) {
            return {allNodes: nodes, taskEdges: edges};
        }

        const mainRootClusterElementNode = {
            data: rootClusterElementNodeData,
            id: rootClusterElementNodeData.workflowNodeName,
            position: DEFAULT_NODE_POSITION,
            type: 'workflow',
        };

        nodes.push(mainRootClusterElementNode);

        const clusterElementNodes = createClusterElementsNodes({
            clusterElements,
            clusterRootId: rootClusterElementNodeData.workflowNodeName,
            currentRootComponentDefinition: mainClusterRootComponentDefinition,
            nestedClusterRootsDefinitions: nestedClusterRootsComponentDefinitions || {},
            operationName: rootClusterElementNodeData.operationName,
        });

        nodes.push(...clusterElementNodes);

        const clusterElementEdges = createClusterElementsEdges({
            clusterRootComponentDefinition: mainClusterRootComponentDefinition,
            clusterRootId: rootClusterElementNodeData.workflowNodeName,
            nestedClusterRootsDefinitions: nestedClusterRootsComponentDefinitions || {},
            nodes,
        });

        edges.push(...clusterElementEdges);

        return {allNodes: nodes, taskEdges: edges};

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        mainClusterRootComponentDefinition,
        nestedClusterRootsComponentDefinitions,
        rootClusterElementNodeData,
        workflow,
    ]);

    const getClusterRootQueryParameters = useCallback(
        (elements: ClusterElementsType): Array<{componentName: string; componentVersion: number}> =>
            Object.values(elements).flatMap((value) => {
                if (Array.isArray(value)) {
                    return value.flatMap((item: ClusterElementItemType) => {
                        if (item.clusterElements) {
                            return [
                                {
                                    componentName: item.type.split('/')[0],
                                    componentVersion: Number(item.type?.split('/')[1]?.replace(/^v/, '')) || 1,
                                },
                                ...getClusterRootQueryParameters(item.clusterElements),
                            ];
                        }

                        return [];
                    });
                } else if (isPlainObject(value)) {
                    if (value.clusterElements) {
                        return [
                            {
                                componentName: value.type.split('/')[0],
                                componentVersion: Number(value.type?.split('/')[1]?.replace(/^v/, '')) || 1,
                            },
                            ...getClusterRootQueryParameters(value.clusterElements),
                        ];
                    }
                }

                return [];
            }),
        []
    );

    const clusterRootQueryParameters = useMemo(
        () => getClusterRootQueryParameters(clusterElements),
        [clusterElements, getClusterRootQueryParameters]
    );

    const getClusterRootDefinitionQuery = useCallback(
        (roots: Array<{componentName: string; componentVersion: number}>) =>
            roots.map((root) => ({
                componentName: root.componentName,
                componentVersion: root.componentVersion,
                queryFn: () =>
                    new ComponentDefinitionApi().getComponentDefinition({
                        componentName: root.componentName,
                        componentVersion: root.componentVersion,
                    }),
                queryKey: ComponentDefinitionKeys.componentDefinition({
                    componentName: root.componentName,
                    componentVersion: root.componentVersion,
                }),
            })),
        []
    );

    useEffect(() => {
        if (
            rootClusterElementDefinition &&
            rootClusterElementNodeData?.workflowNodeName &&
            !isNodeDragging &&
            !isPositionSaving
        ) {
            setMainClusterRootComponentDefinition(rootClusterElementDefinition);
        }
    }, [
        rootClusterElementDefinition,
        rootClusterElementNodeData?.workflowNodeName,
        setMainClusterRootComponentDefinition,
        isNodeDragging,
        isPositionSaving,
    ]);

    useEffect(() => {
        const processClusterElementsRequirementsMet =
            !!rootClusterElementNodeData &&
            !!rootClusterElementDefinition &&
            !!workflow.definition &&
            Boolean(Object.keys(clusterElements).length > 0) &&
            clusterRootQueryParameters.length;

        if (!processClusterElementsRequirementsMet) {
            return;
        }

        const getNestedClusterRootsComponentDefinitions = async () => {
            try {
                const clusterRootDefinitionQueries = getClusterRootDefinitionQuery(clusterRootQueryParameters);
                const nestedDefinitions: Record<string, NestedClusterRootComponentDefinitionType> = {};

                for (const query of clusterRootDefinitionQueries) {
                    if (!nestedDefinitions[query.componentName]) {
                        const definition = await queryClient.fetchQuery({
                            queryFn: query.queryFn,
                            queryKey: query.queryKey,
                        });

                        const trimmedDefinition = {
                            actionClusterElementTypes: definition.actionClusterElementTypes || {},
                            clusterElementClusterElementTypes: definition.clusterElementClusterElementTypes || {},
                            clusterElementTypes: definition.clusterElementTypes || [],
                        };

                        nestedDefinitions[query.componentName] = trimmedDefinition;
                    }
                }

                setNestedClusterRootsComponentDefinitions(nestedDefinitions);
            } catch (error) {
                console.error('Error fetching nested cluster root definitions:', error);
            }
        };

        getNestedClusterRootsComponentDefinitions();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        clusterElements,
        queryClient,
        workflow.definition,
        clusterRootQueryParameters,
        getClusterRootDefinitionQuery,
        setNestedClusterRootsComponentDefinitions,
        rootClusterElementDefinition,
    ]);

    useEffect(() => {
        if (isNodeDragging || isPositionSaving) {
            return;
        }

        const layoutNodes = allNodes;
        const edges: Edge[] = taskEdges;

        if (layoutNodes.length === 0) {
            return;
        }

        getLayoutedElements({
            canvasWidth,
            edges,
            isClusterElementsCanvas: !!rootClusterElementNodeData,
            nodes: layoutNodes,
        }).then((elements) => {
            setNodes(elements.nodes);
            setEdges(elements.edges);
        });

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, rootClusterElementNodeData, allNodes]);
};

export default useClusterElementsLayout;
