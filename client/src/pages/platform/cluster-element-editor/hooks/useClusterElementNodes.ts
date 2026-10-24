import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {ComponentDefinitionApi} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClusterElementItemType, ClusterElementsType, NestedClusterRootComponentDefinitionType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {Edge, Node} from '@xyflow/react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsViewMode from '../../workflow-editor/hooks/useClusterElementsViewMode';
import useWorkflowDataStore from '../../workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import collectClusterElementsSignature from '../../workflow-editor/utils/collectClusterElementsSignature';
import {getTask} from '../../workflow-editor/utils/getTask';
import {getFilteredClusterElementTypes, isPlainObject} from '../utils/clusterElementsUtils';
import createClusterElementsEdges from '../utils/createClusterElementsEdges';
import createClusterElementsNodes from '../utils/createClusterElementsNodes';

interface UseClusterElementNodesResultI {
    definitionsReady: boolean;
    edgesByRootId: Record<string, Edge[]>;
    nodesByRootId: Record<string, Node[]>;
}

const EMPTY_EDGES_BY_ROOT_ID: Record<string, Edge[]> = {};
const EMPTY_NODES_BY_ROOT_ID: Record<string, Node[]> = {};

function rootFailureKey(clusterRootId: string): string {
    return `root:${clusterRootId}`;
}

function nestedFailureKey(componentName: string): string {
    return `nested:${componentName}`;
}

export default function useClusterElementNodes(
    clusterRootIds: string[],
    readOnlyDefinition?: string
): UseClusterElementNodesResultI {
    const {
        clusterRootComponentDefinitions,
        nestedClusterRootsComponentDefinitions,
        rootClusterElementNodeData,
        setClusterRootComponentDefinition,
        setNestedClusterRootsComponentDefinitions,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterRootComponentDefinitions: state.clusterRootComponentDefinitions,
            nestedClusterRootsComponentDefinitions: state.nestedClusterRootsComponentDefinitions,
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setClusterRootComponentDefinition: state.setClusterRootComponentDefinition,
            setNestedClusterRootsComponentDefinitions: state.setNestedClusterRootsComponentDefinitions,
        }))
    );
    const clusterElementsViewMode = useClusterElementsViewMode();

    const {workflow: editorWorkflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const [failedDefinitionKeys, setFailedDefinitionKeys] = useState<Record<string, true>>({});

    const requestedRootDefinitionKeysRef = useRef(new Set<string>());

    const workflow = readOnlyDefinition === undefined ? editorWorkflow : {definition: readOnlyDefinition};

    const queryClient = useQueryClient();

    const clusterRootIdsKey = clusterRootIds.join(',');

    const workflowDefinitionTasks = useMemo(() => {
        if (!workflow.definition) {
            return [];
        }

        return JSON.parse(workflow.definition).tasks || [];
    }, [workflow.definition]);

    const clusterElementsSignature = useMemo(() => {
        if (!workflow.definition) {
            return '';
        }

        return clusterRootIds
            .map((clusterRootId) => {
                const task = getTask({tasks: workflowDefinitionTasks, workflowNodeName: clusterRootId});

                return task
                    ? `${clusterRootId}:${task.type}:${collectClusterElementsSignature(task.clusterElements)}`
                    : `${clusterRootId}:absent`;
            })
            .join('|');
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [clusterRootIdsKey, workflow.definition, workflowDefinitionTasks]);

    const clusterElementsByRootId = useMemo(() => {
        const clusterElementsMap: Record<string, ClusterElementsType | undefined> = {};

        for (const clusterRootId of clusterRootIds) {
            const task = getTask({tasks: workflowDefinitionTasks, workflowNodeName: clusterRootId});

            clusterElementsMap[clusterRootId] = task ? task.clusterElements || {} : undefined;
        }

        return clusterElementsMap;
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [clusterElementsSignature]);

    const getClusterRootQueryParameters = useCallback(
        (elements: ClusterElementsType): Array<{componentName: string; componentVersion: number}> =>
            Object.values(elements).flatMap((value) => {
                if (Array.isArray(value)) {
                    return value.flatMap((item: ClusterElementItemType) => [
                        {
                            componentName: item.type.split('/')[0],
                            componentVersion: Number(item.type?.split('/')[1]?.replace(/^v/, '')) || 1,
                        },
                        ...getClusterRootQueryParameters(item.clusterElements ?? {}),
                    ]);
                } else if (isPlainObject(value)) {
                    return [
                        {
                            componentName: value.type.split('/')[0],
                            componentVersion: Number(value.type?.split('/')[1]?.replace(/^v/, '')) || 1,
                        },
                        ...getClusterRootQueryParameters(value.clusterElements ?? {}),
                    ];
                }

                return [];
            }),
        []
    );

    const clusterRootQueryParameters = useMemo(
        () =>
            clusterRootIds.flatMap((clusterRootId) =>
                getClusterRootQueryParameters(clusterElementsByRootId[clusterRootId] ?? {})
            ),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [clusterElementsByRootId, clusterRootIdsKey, getClusterRootQueryParameters]
    );

    const rootComponentQueryParameters = useMemo(() => {
        const parameters: Array<{clusterRootId: string; componentName: string; componentVersion: number}> = [];

        for (const clusterRootId of clusterRootIds) {
            if (failedDefinitionKeys[rootFailureKey(clusterRootId)]) {
                continue;
            }

            const task = getTask({tasks: workflowDefinitionTasks, workflowNodeName: clusterRootId});

            const cachedDefinition = clusterRootComponentDefinitions[clusterRootId];

            if (!task || (cachedDefinition && !task.type)) {
                continue;
            }

            const componentName = task.type.split('/')[0];
            const componentVersion = Number(task.type?.split('/')[1]?.replace(/^v/, '')) || 1;

            const requestedRootDefinitionKey = `${clusterRootId}:${componentName}/${componentVersion}`;

            if (
                cachedDefinition &&
                ((cachedDefinition.name === componentName && cachedDefinition.version === componentVersion) ||
                    requestedRootDefinitionKeysRef.current.has(requestedRootDefinitionKey))
            ) {
                continue;
            }

            requestedRootDefinitionKeysRef.current.add(requestedRootDefinitionKey);

            parameters.push({clusterRootId, componentName, componentVersion});
        }

        return parameters;
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [clusterElementsSignature, clusterRootComponentDefinitions, clusterRootIdsKey, failedDefinitionKeys]);

    const missingNestedQueryParameters = useMemo(() => {
        const missingParametersByComponentName = new Map<string, {componentName: string; componentVersion: number}>();

        for (const parameter of clusterRootQueryParameters) {
            if (
                missingParametersByComponentName.has(parameter.componentName) ||
                nestedClusterRootsComponentDefinitions?.[parameter.componentName] !== undefined ||
                failedDefinitionKeys[nestedFailureKey(parameter.componentName)]
            ) {
                continue;
            }

            missingParametersByComponentName.set(parameter.componentName, parameter);
        }

        return Array.from(missingParametersByComponentName.values());
    }, [clusterRootQueryParameters, failedDefinitionKeys, nestedClusterRootsComponentDefinitions]);

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

    const {edgesByRootId, nodesByRootId} = useMemo(() => {
        if (clusterRootIdsKey === '') {
            return {edgesByRootId: EMPTY_EDGES_BY_ROOT_ID, nodesByRootId: EMPTY_NODES_BY_ROOT_ID};
        }

        const edges: Record<string, Edge[]> = {};
        const nodes: Record<string, Node[]> = {};

        for (const clusterRootId of clusterRootIds) {
            const clusterElements = clusterElementsByRootId[clusterRootId];
            const currentRootComponentDefinition = clusterRootComponentDefinitions[clusterRootId];

            nodes[clusterRootId] = [];
            edges[clusterRootId] = [];

            if (!clusterElements || !currentRootComponentDefinition) {
                continue;
            }

            const rootNodes: Node[] = [];

            if (
                clusterElementsViewMode === 'dialog' &&
                rootClusterElementNodeData?.workflowNodeName === clusterRootId
            ) {
                const rootFilteredTypes = getFilteredClusterElementTypes({
                    clusterRootComponentDefinition: currentRootComponentDefinition,
                    isNestedClusterRoot: false,
                    operationName: rootClusterElementNodeData.operationName,
                });

                rootNodes.push({
                    data: {
                        ...rootClusterElementNodeData,
                        clusterElementTypesCount: rootFilteredTypes.length,
                    },
                    id: clusterRootId,
                    position: DEFAULT_NODE_POSITION,
                    type: 'workflow',
                });
            }

            const clusterElementNodes = createClusterElementsNodes({
                clusterElements,
                clusterRootId,
                currentRootComponentDefinition,
                nestedClusterRootsDefinitions: nestedClusterRootsComponentDefinitions || {},
                operationName: rootClusterElementNodeData?.operationName,
            });

            const rootAndElementNodes = [...rootNodes, ...clusterElementNodes];

            nodes[clusterRootId] = rootAndElementNodes;

            edges[clusterRootId] = createClusterElementsEdges({
                clusterRootComponentDefinition: currentRootComponentDefinition,
                clusterRootId,
                nestedClusterRootsDefinitions: nestedClusterRootsComponentDefinitions || {},
                nodes: rootAndElementNodes,
            });
        }

        return {edgesByRootId: edges, nodesByRootId: nodes};
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        clusterElementsByRootId,
        clusterElementsViewMode,
        clusterRootComponentDefinitions,
        clusterRootIdsKey,
        nestedClusterRootsComponentDefinitions,
        rootClusterElementNodeData,
    ]);

    const definitionsReady = useMemo(() => {
        const requiredComponentNames = new Set(clusterRootQueryParameters.map((parameter) => parameter.componentName));

        const nestedDefinitionsReady = Array.from(requiredComponentNames).every(
            (componentName) =>
                nestedClusterRootsComponentDefinitions?.[componentName] !== undefined ||
                failedDefinitionKeys[nestedFailureKey(componentName)]
        );

        return nestedDefinitionsReady && rootComponentQueryParameters.length === 0;
    }, [
        clusterRootQueryParameters,
        failedDefinitionKeys,
        nestedClusterRootsComponentDefinitions,
        rootComponentQueryParameters,
    ]);

    useEffect(() => {
        const processClusterElementsRequirementsMet =
            missingNestedQueryParameters.length > 0 || rootComponentQueryParameters.length > 0;

        if (!processClusterElementsRequirementsMet) {
            return;
        }

        const getClusterRootComponentDefinitions = async () => {
            const missingNestedQueries = getClusterRootDefinitionQuery(missingNestedQueryParameters);

            const resolvedNestedDefinitions: Record<string, NestedClusterRootComponentDefinitionType> = {};
            const newlyFailedComponentNames: string[] = [];

            await Promise.all(
                missingNestedQueries.map(async ({componentName, ...query}) => {
                    try {
                        const definition = await queryClient.query({
                            queryFn: query.queryFn,
                            queryKey: query.queryKey,
                        });

                        resolvedNestedDefinitions[componentName] = {
                            actionClusterElementTypes: definition.actionClusterElementTypes || {},
                            clusterElementClusterElementTypes: definition.clusterElementClusterElementTypes || {},
                            clusterElementTypes: definition.clusterElementTypes || [],
                        };
                    } catch (error) {
                        console.error(
                            `Error fetching nested cluster root definition for component "${componentName}":`,
                            error
                        );

                        newlyFailedComponentNames.push(componentName);
                    }
                })
            );

            if (Object.keys(resolvedNestedDefinitions).length > 0) {
                setNestedClusterRootsComponentDefinitions({
                    ...useWorkflowEditorStore.getState().nestedClusterRootsComponentDefinitions,
                    ...resolvedNestedDefinitions,
                });
            }

            if (newlyFailedComponentNames.length > 0) {
                setFailedDefinitionKeys((previousFailedDefinitionKeys) => {
                    const nextFailedDefinitionKeys = {...previousFailedDefinitionKeys};

                    for (const failedComponentName of newlyFailedComponentNames) {
                        nextFailedDefinitionKeys[nestedFailureKey(failedComponentName)] = true;
                    }

                    return nextFailedDefinitionKeys;
                });
            }

            const rootDefinitionFetches = rootComponentQueryParameters.map((parameter) => ({
                clusterRootId: parameter.clusterRootId,
                queryFn: () =>
                    new ComponentDefinitionApi().getComponentDefinition({
                        componentName: parameter.componentName,
                        componentVersion: parameter.componentVersion,
                    }),
                queryKey: ComponentDefinitionKeys.componentDefinition({
                    componentName: parameter.componentName,
                    componentVersion: parameter.componentVersion,
                }),
            }));

            const newlyFailedClusterRootIds: string[] = [];

            await Promise.all(
                rootDefinitionFetches.map(async (rootDefinitionFetch) => {
                    try {
                        const definition = await queryClient.query({
                            queryFn: rootDefinitionFetch.queryFn,
                            queryKey: rootDefinitionFetch.queryKey,
                        });

                        setClusterRootComponentDefinition(rootDefinitionFetch.clusterRootId, definition);
                    } catch (error) {
                        console.error(
                            `Error fetching component definition for cluster root "${rootDefinitionFetch.clusterRootId}":`,
                            error
                        );

                        newlyFailedClusterRootIds.push(rootDefinitionFetch.clusterRootId);
                    }
                })
            );

            if (newlyFailedClusterRootIds.length > 0) {
                setFailedDefinitionKeys((previousFailedDefinitionKeys) => {
                    const nextFailedDefinitionKeys = {...previousFailedDefinitionKeys};

                    for (const failedClusterRootId of newlyFailedClusterRootIds) {
                        nextFailedDefinitionKeys[rootFailureKey(failedClusterRootId)] = true;
                    }

                    return nextFailedDefinitionKeys;
                });
            }
        };

        void getClusterRootComponentDefinitions();
    }, [
        missingNestedQueryParameters,
        rootComponentQueryParameters,
        getClusterRootDefinitionQuery,
        queryClient,
        setClusterRootComponentDefinition,
        setNestedClusterRootsComponentDefinitions,
    ]);

    return {definitionsReady, edgesByRootId, nodesByRootId};
}
