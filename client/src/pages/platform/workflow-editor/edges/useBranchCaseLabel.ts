import {LayoutDirectionType} from '@/shared/constants';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {branchCaseKeysMatch} from '../utils/layoutUtils';
import saveRootTaskDispatcher from '../utils/saveRootTaskDispatcher';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';
import computeBranchCaseLabelPosition from './computeBranchCaseLabelPosition';

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

    const queryClient = useQueryClient();
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

            saveRootTaskDispatcher({
                invalidateWorkflowQueries: invalidateWorkflowQueries!,
                nodes,
                parentNodeData: parentBranchNodeData,
                queryClient,
                updateWorkflowMutation: updateWorkflowMutation!,
                updatedParameters: branchParameters,
                workflowDefinition: workflow.definition,
            });
        },
        [
            invalidateWorkflowQueries,
            nodes,
            parentBranchNodeData,
            queryClient,
            updateWorkflowMutation,
            workflow.definition,
        ]
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
