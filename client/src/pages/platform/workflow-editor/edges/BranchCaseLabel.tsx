import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {EdgeLabelRenderer} from '@xyflow/react';
import {CheckIcon, PenIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

interface BranchCaseLabelProps {
    caseKey: string;
    edgeId: string;
    sourceY: number;
    targetX: number;
}

export default function BranchCaseLabel({caseKey, edgeId, sourceY, targetX}: BranchCaseLabelProps) {
    const [isCaseKeyEditable, setIsCaseKeyEditable] = useState(false);
    const [isDeleteConfirmationVisible, setIsDeleteConfirmationVisible] = useState(false);
    const [caseKeyValue, setCaseKeyValue] = useState(caseKey);

    const inputRef = useRef<HTMLInputElement>(null);

    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const queryClient = useQueryClient();
    const {projectId} = useParams();
    const {updateWorkflowMutation} = useWorkflowMutation();

    const targetNodeId = useMemo(() => edgeId.split('=>')[1], [edgeId]);

    const targetNodeData = useMemo(
        () => nodes.find((node) => node.id === targetNodeId)?.data as NodeDataType,
        [nodes, targetNodeId]
    );
    const parentBranchNodeId = targetNodeData.branchData ? targetNodeData.branchData.branchId : targetNodeData.branchId;

    const parentBranchNodeData = nodes.find((node) => node.id === parentBranchNodeId)?.data as NodeDataType;

    const branchCases: BranchCaseType[] = parentBranchNodeData?.parameters?.cases;

    const lastBranchCaseKey = branchCases?.[branchCases?.length - 1]?.key;

    const isLastCase = caseKey === lastBranchCaseKey;

    const isDefaultCase = !branchCases?.find((branchCase) => branchCase.key === caseKey);

    const saveBranchChange = useCallback(
        (branchParameters: object) => {
            saveWorkflowDefinition({
                nodeData: {
                    ...parentBranchNodeData,
                    parameters: branchParameters,
                },
                projectId: Number(projectId),
                queryClient,
                updateWorkflowMutation,
            });
        },
        [parentBranchNodeData, projectId, queryClient, updateWorkflowMutation]
    );

    const handleCreateCaseClick = useCallback(() => {
        if (!parentBranchNodeData.parameters?.cases) {
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
        (caseKeyToDelete: string) => {
            if (!parentBranchNodeData) {
                return;
            }

            const newCases = (parentBranchNodeData.parameters?.cases as BranchCaseType[]).filter(
                (branchCase) => branchCase.key !== caseKeyToDelete
            );

            saveBranchChange({
                ...parentBranchNodeData.parameters,
                cases: newCases,
            });
        },
        [parentBranchNodeData, saveBranchChange]
    );

    const handleEditCaseClick = useCallback(() => {
        const caseKeyEditable = !isCaseKeyEditable;

        setIsCaseKeyEditable(caseKeyEditable);

        if (caseKeyEditable) {
            setTimeout(() => {
                if (inputRef.current) {
                    inputRef.current.focus();

                    inputRef.current.select();
                }
            }, 0);
        }
    }, [isCaseKeyEditable]);

    const handleSaveCaseClick = useCallback(() => {
        const caseKeyEditable = !isCaseKeyEditable;

        if (!caseKeyEditable) {
            const isDuplicate =
                caseKeyValue !== caseKey &&
                (parentBranchNodeData.parameters?.cases as BranchCaseType[]).some(
                    (branchCase) => branchCase.key === caseKeyValue
                );

            if (isDuplicate || caseKeyValue === 'default') {
                setIsCaseKeyEditable(false);

                setCaseKeyValue(caseKey);

                return;
            }
        }

        setIsCaseKeyEditable(caseKeyEditable);

        const newCases = (parentBranchNodeData.parameters?.cases as BranchCaseType[]).map((branchCase) => {
            if (branchCase.key === caseKey) {
                return {
                    ...branchCase,
                    key: caseKeyValue,
                };
            }

            return branchCase;
        });

        if (!caseKeyValue) {
            setCaseKeyValue(caseKey);

            return;
        }

        saveBranchChange({
            ...parentBranchNodeData.parameters,
            cases: newCases,
        });
    }, [caseKey, caseKeyValue, isCaseKeyEditable, parentBranchNodeData.parameters, saveBranchChange]);

    useEffect(() => {
        setCaseKeyValue(caseKey);
    }, [caseKey]);

    return (
        <EdgeLabelRenderer key={`${edgeId}-case-label`}>
            <div
                className="top-6 z-10 flex items-center rounded-md border-2 border-stroke-neutral-tertiary bg-white p-1 text-xs font-medium shadow-sm hover:border-stroke-brand-secondary-hover"
                style={{
                    pointerEvents: 'all',
                    position: 'absolute',
                    transform: `translate(${targetX}px, ${sourceY}px) translate(-50%, -50%)`,
                }}
            >
                {isDefaultCase && <span className="p-1 text-xs">default</span>}

                {!isDefaultCase && (
                    <div className="group relative flex items-center">
                        <Input
                            className="h-auto max-w-24 border-none pr-7 text-xs shadow-none disabled:cursor-auto disabled:text-xs disabled:opacity-100 group-hover:bg-surface-brand-secondary md:text-xs"
                            disabled={!isCaseKeyEditable}
                            onChange={(event) => setCaseKeyValue(event.target.value)}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter' && isCaseKeyEditable) {
                                    handleSaveCaseClick();
                                }
                            }}
                            ref={inputRef}
                            value={caseKeyValue}
                        />

                        <Button
                            className="absolute right-2 top-1/2 size-4 -translate-y-1/2 cursor-pointer text-content-neutral-primary/50 hover:bg-transparent hover:text-content-neutral-primary [&_svg]:size-3"
                            onClick={isCaseKeyEditable ? handleSaveCaseClick : handleEditCaseClick}
                            size="icon"
                            variant="ghost"
                        >
                            {isCaseKeyEditable ? <CheckIcon className="text-content-brand-primary" /> : <PenIcon />}
                        </Button>
                    </div>
                )}

                {!isDefaultCase && branchCases.length > 1 && (
                    <Button
                        className="ml-1 size-auto cursor-pointer p-1 text-content-destructive/50 hover:bg-surface-destructive-secondary hover:text-content-destructive [&_svg]:size-4"
                        onClick={
                            isDeleteConfirmationVisible
                                ? () => handleDeleteCaseClick(caseKey)
                                : () => setIsDeleteConfirmationVisible(true)
                        }
                        size="icon"
                        variant="ghost"
                    >
                        {isDeleteConfirmationVisible ? <CheckIcon /> : <TrashIcon />}
                    </Button>
                )}

                {isLastCase && (
                    <Button
                        className="ml-1 size-auto cursor-pointer p-1 text-content-neutral-primary/50 hover:bg-surface-neutral-primary-hover hover:text-content-neutral-primary [&_svg]:size-4"
                        onClick={() => handleCreateCaseClick()}
                        size="icon"
                        variant="ghost"
                    >
                        <PlusIcon />
                    </Button>
                )}
            </div>
        </EdgeLabelRenderer>
    );
}
