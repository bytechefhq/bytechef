import {LayoutDirectionType, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, NodeDataType, TaskDispatcherContextType, TaskDispatcherDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {branchCaseKeysMatch} from '../utils/layoutUtils';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';
import computeBranchCaseLabelPosition from './computeBranchCaseLabelPosition';

function getTaskDispatcherPropertyName(taskDispatcherName: string): string {
    switch (taskDispatcherName) {
        case 'fork-join':
            return 'forkJoin';
        default:
            return taskDispatcherName;
    }
}

function getRecursivelyUpdatedRootTaskDispatcherNodeData(
    currentTaskNodeData: NodeDataType,
    definitionTasks: WorkflowTask[],
    nodesMap: Map<string, Node>
): NodeDataType {
    const taskName = currentTaskNodeData.name || currentTaskNodeData.workflowNodeName;
    const nodeData: NodeDataType = (nodesMap.get(taskName)?.data as NodeDataType) || currentTaskNodeData;

    let parentTaskDispatcherInfo: {
        componentName: string;
        context: TaskDispatcherDataType;
        task: WorkflowTask;
    } | null = null;

    for (const taskDispatcherName of TASK_DISPATCHER_NAMES) {
        const propertyName = getTaskDispatcherPropertyName(taskDispatcherName);

        const taskDispatcherData = nodeData[
            `${propertyName}Data` as keyof typeof nodeData
        ] as TaskDispatcherContextType;

        if (!taskDispatcherData) {
            continue;
        }

        const taskDispatcherId: string = (taskDispatcherData as unknown as Record<string, string>)[`${propertyName}Id`];

        const taskDispatcherConfig = TASK_DISPATCHER_CONFIG[taskDispatcherName as keyof typeof TASK_DISPATCHER_CONFIG];

        if (!taskDispatcherId || !taskDispatcherConfig) {
            continue;
        }

        const parentTaskDispatcherTask: WorkflowTask = taskDispatcherConfig.getTask({
            taskDispatcherId,
            tasks: definitionTasks,
        })!;

        if (parentTaskDispatcherTask) {
            parentTaskDispatcherInfo = {
                componentName: taskDispatcherName,
                context: {...taskDispatcherData} as TaskDispatcherDataType,
                task: parentTaskDispatcherTask,
            };

            break;
        }
    }

    if (!parentTaskDispatcherInfo) {
        return currentTaskNodeData;
    }

    const parentTaskDispatcherNode = nodesMap.get(parentTaskDispatcherInfo.task.name);

    if (!parentTaskDispatcherNode) {
        return currentTaskNodeData;
    }

    const taskDispatcherConfig =
        TASK_DISPATCHER_CONFIG[parentTaskDispatcherInfo.componentName as keyof typeof TASK_DISPATCHER_CONFIG];

    const subtasks = taskDispatcherConfig.getSubtasks({
        context: {
            ...(parentTaskDispatcherInfo.context as TaskDispatcherContextType),
        },
        task: parentTaskDispatcherInfo.task,
    });

    const taskIndex = subtasks.findIndex((task) => task.name === taskName);

    if (taskIndex >= 0) {
        const updatedSubtasks = [...subtasks];

        const cleanTaskUpdate = {
            ...updatedSubtasks[taskIndex],
            parameters: currentTaskNodeData.parameters,
        };

        updatedSubtasks[taskIndex] = cleanTaskUpdate;

        const updatedParentTask = taskDispatcherConfig.updateTaskParameters({
            context: {...(parentTaskDispatcherInfo.context as TaskDispatcherContextType)},
            task: parentTaskDispatcherInfo.task,
            updatedSubtasks,
        });

        const parentNodeData = parentTaskDispatcherNode.data as NodeDataType;

        const updatedParentNodeData: NodeDataType = {
            ...parentNodeData,
            ...updatedParentTask,
        };

        return getRecursivelyUpdatedRootTaskDispatcherNodeData(updatedParentNodeData, definitionTasks, nodesMap);
    }

    return currentTaskNodeData;
}

interface UseBranchCaseLabelProps {
    caseKey: string | number;
    edgeId: string;
    hasEdgeButton?: boolean;
    layoutDirection: LayoutDirectionType;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

function parseCaseKeyValue(value: string): string | number {
    const trimmedValue = value.trim();

    if (/^-?\d*\.?\d+$/.test(trimmedValue)) {
        const numericValue = parseFloat(trimmedValue);

        if (trimmedValue !== 'default') {
            return numericValue;
        }
    }

    return trimmedValue;
}

export default function useBranchCaseLabel({
    caseKey,
    edgeId,
    hasEdgeButton,
    layoutDirection,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: UseBranchCaseLabelProps) {
    const [isCaseKeyEditable, setIsCaseKeyEditable] = useState(false);
    const [isDeleteConfirmationVisible, setIsDeleteConfirmationVisible] = useState(false);
    const [caseKeyValue, setCaseKeyValue] = useState(String(caseKey));

    const inputRef = useRef<HTMLInputElement>(null);

    const labelPosition = useMemo(
        () => computeBranchCaseLabelPosition({hasEdgeButton, layoutDirection, sourceX, sourceY, targetX, targetY}),
        [hasEdgeButton, layoutDirection, sourceX, sourceY, targetX, targetY]
    );

    const {invalidateWorkflowQueries} = useWorkflowEditor();

    const {nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            workflow: state.workflow,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const targetNodeId = useMemo(() => edgeId.split('=>')[1], [edgeId]);

    const targetNodeData = useMemo(
        () => nodes.find((node) => node.id === targetNodeId)?.data as NodeDataType,
        [nodes, targetNodeId]
    );
    const parentBranchNodeId = targetNodeData.branchData ? targetNodeData.branchData.branchId : targetNodeData.branchId;

    const parentBranchNodeData = nodes.find((node) => node.id === parentBranchNodeId)?.data as NodeDataType;

    const branchCases: BranchCaseType[] = parentBranchNodeData?.parameters?.cases;

    const lastBranchCaseKey = branchCases?.[branchCases?.length - 1]?.key;

    const isLastCase = lastBranchCaseKey !== undefined && branchCaseKeysMatch(caseKey, lastBranchCaseKey);

    const isDefaultCase = !branchCases?.find((branchCase) => branchCaseKeysMatch(branchCase.key, caseKey));

    const saveBranchChange = useCallback(
        (branchParameters: object) => {
            if (!workflow.definition || !parentBranchNodeData) {
                return;
            }

            const workflowDefinition = workflow.definition;
            const workflowDefinitionTasks = JSON.parse(workflowDefinition).tasks;
            const nodesMap: Map<string, Node> = new Map();

            nodes.forEach((node) => {
                const {data, id} = node;

                nodesMap.set(id, {
                    ...node,
                    data: {
                        ...data,
                        ...TASK_DISPATCHER_NAMES.reduce((nodeData: {[key: string]: unknown}, taskDispatcherName) => {
                            const key = `${taskDispatcherName}Data`;

                            if (data[key]) {
                                nodeData[key] = data[key];
                            }

                            return nodeData;
                        }, {}),
                    } as NodeDataType,
                    id,
                });
            });

            const taskNode = nodes.find(
                (node) => node.id === parentBranchNodeData?.name || node.data.name === parentBranchNodeData?.name
            );

            if (!taskNode) {
                return;
            }

            const updatedTaskNodeData: NodeDataType = {
                ...(taskNode.data as NodeDataType),
                parameters: branchParameters,
            };

            const rootNodeData = getRecursivelyUpdatedRootTaskDispatcherNodeData(
                updatedTaskNodeData,
                workflowDefinitionTasks,
                nodesMap
            );

            saveWorkflowDefinition({
                invalidateWorkflowQueries: invalidateWorkflowQueries!,
                nodeData: rootNodeData,
                updateWorkflowMutation: updateWorkflowMutation!,
            });
        },
        [invalidateWorkflowQueries, nodes, parentBranchNodeData, updateWorkflowMutation, workflow.definition]
    );

    const handleCreateCaseClick = useCallback(() => {
        if (!parentBranchNodeData?.parameters?.cases) {
            return;
        }

        const newCaseKey = TASK_DISPATCHER_CONFIG.branch.getNewCaseKey(parentBranchNodeData.parameters?.cases);

        saveBranchChange({
            ...parentBranchNodeData.parameters,
            cases: [
                ...(parentBranchNodeData.parameters?.cases || []),
                {
                    key: newCaseKey,
                    tasks: [],
                },
            ],
        });
    }, [parentBranchNodeData, saveBranchChange]);

    const handleDeleteCaseClick = useCallback(
        (caseKeyToDelete: string | number) => {
            const parentBranchCases: BranchCaseType[] = parentBranchNodeData?.parameters?.cases;

            if (!parentBranchCases) {
                return;
            }

            const newCases = parentBranchCases.filter(
                (branchCase) => !branchCaseKeysMatch(branchCase.key, caseKeyToDelete)
            );

            saveBranchChange({
                ...parentBranchNodeData.parameters,
                cases: newCases,
            });
        },
        [parentBranchNodeData, saveBranchChange]
    );

    const handleEditCaseClick = useCallback(() => {
        setIsCaseKeyEditable(!isCaseKeyEditable);

        if (!isCaseKeyEditable) {
            setTimeout(() => {
                inputRef.current?.focus();

                inputRef.current?.select();
            }, 0);
        }
    }, [isCaseKeyEditable]);

    const handleSaveCaseClick = useCallback(() => {
        if (!isCaseKeyEditable) {
            return;
        }

        const parentBranchCases: BranchCaseType[] = parentBranchNodeData.parameters?.cases;

        const parsedCaseKey = parseCaseKeyValue(caseKeyValue);

        const isDuplicate =
            !branchCaseKeysMatch(parsedCaseKey, caseKey) &&
            (parentBranchCases || []).some((branchCase) => branchCaseKeysMatch(branchCase.key, parsedCaseKey));

        if (isDuplicate || caseKeyValue === 'default' || !caseKeyValue) {
            setIsCaseKeyEditable(false);

            setCaseKeyValue(String(caseKey));

            return;
        }

        const newCases = (parentBranchCases || []).map((branchCase) => {
            if (branchCaseKeysMatch(branchCase.key, caseKey)) {
                return {
                    ...branchCase,
                    key: parsedCaseKey,
                };
            }

            return branchCase;
        });

        saveBranchChange({
            ...parentBranchNodeData.parameters,
            cases: newCases,
        });

        setIsCaseKeyEditable(false);
    }, [caseKey, caseKeyValue, isCaseKeyEditable, parentBranchNodeData?.parameters, saveBranchChange]);

    useEffect(() => setCaseKeyValue(String(caseKey)), [caseKey]);

    const handleDeleteButtonClick = useCallback(() => {
        if (isDeleteConfirmationVisible) {
            handleDeleteCaseClick(caseKey);
        } else {
            setIsDeleteConfirmationVisible(true);
        }
    }, [caseKey, handleDeleteCaseClick, isDeleteConfirmationVisible]);

    return {
        branchCases,
        caseKeyValue,
        handleCreateCaseClick,
        handleDeleteButtonClick,
        handleEditCaseClick,
        handleSaveCaseClick,
        inputRef,
        isCaseKeyEditable,
        isDefaultCase,
        isDeleteConfirmationVisible,
        isLastCase,
        labelPosition,
        setCaseKeyValue,
    };
}
