import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {EdgeLabelRenderer} from '@xyflow/react';
import {CheckIcon, PenIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import saveRootTaskDispatcher from '../utils/saveRootTaskDispatcher';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

interface BranchCaseLabelProps {
    caseKey: string | number;
    edgeId: string;
    sourceY: number;
    targetX: number;
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

export default function BranchCaseLabel({caseKey, edgeId, sourceY, targetX}: BranchCaseLabelProps) {
    const [isCaseKeyEditable, setIsCaseKeyEditable] = useState(false);
    const [isDeleteConfirmationVisible, setIsDeleteConfirmationVisible] = useState(false);
    const [caseKeyValue, setCaseKeyValue] = useState(String(caseKey));

    const inputRef = useRef<HTMLInputElement>(null);

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

    const isLastCase = caseKey === lastBranchCaseKey;

    const isDefaultCase = !branchCases?.find((branchCase) => branchCase.key === caseKey);

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

            const newCases = parentBranchCases.filter((branchCase) => branchCase.key !== caseKeyToDelete);

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
            parsedCaseKey !== caseKey &&
            (parentBranchCases || []).some((branchCase) => branchCase.key === parsedCaseKey);

        if (isDuplicate || caseKeyValue === 'default' || !caseKeyValue) {
            setIsCaseKeyEditable(false);

            setCaseKeyValue(String(caseKey));

            return;
        }

        const newCases = (parentBranchCases || []).map((branchCase) => {
            if (branchCase.key === caseKey) {
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
                            icon={
                                isCaseKeyEditable ? <CheckIcon className="text-content-brand-primary" /> : <PenIcon />
                            }
                            onClick={isCaseKeyEditable ? handleSaveCaseClick : handleEditCaseClick}
                            size="icon"
                            variant="ghost"
                        />
                    </div>
                )}

                {!isDefaultCase && branchCases?.length > 1 && (
                    <Button
                        className="ml-1 size-auto cursor-pointer p-1 text-content-destructive/50 hover:bg-surface-destructive-secondary hover:text-content-destructive [&_svg]:size-4"
                        icon={isDeleteConfirmationVisible ? <CheckIcon /> : <TrashIcon />}
                        onClick={
                            isDeleteConfirmationVisible
                                ? () => handleDeleteCaseClick(caseKey)
                                : () => setIsDeleteConfirmationVisible(true)
                        }
                        size="icon"
                        variant="ghost"
                    />
                )}

                {isLastCase && (
                    <Button
                        className="ml-1 size-auto cursor-pointer p-1 text-content-neutral-primary/50 hover:bg-surface-neutral-primary-hover hover:text-content-neutral-primary [&_svg]:size-4"
                        icon={<PlusIcon />}
                        onClick={handleCreateCaseClick}
                        size="icon"
                        variant="ghost"
                    />
                )}
            </div>
        </EdgeLabelRenderer>
    );
}
