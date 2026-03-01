import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {LayoutDirectionType} from '@/shared/constants';
import {EdgeLabelRenderer} from '@xyflow/react';
import {CheckIcon, PenIcon, PlusIcon, TrashIcon} from 'lucide-react';

import useBranchCaseLabel from './useBranchCaseLabel';

interface BranchCaseLabelProps {
    caseKey: string | number;
    edgeId: string;
    hasEdgeButton?: boolean;
    layoutDirection: LayoutDirectionType;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

export default function BranchCaseLabel({
    caseKey,
    edgeId,
    hasEdgeButton,
    layoutDirection,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: BranchCaseLabelProps) {
    const {
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
    } = useBranchCaseLabel({
        caseKey,
        edgeId,
        hasEdgeButton,
        layoutDirection,
        sourceX,
        sourceY,
        targetX,
        targetY,
    });

    return (
        <EdgeLabelRenderer key={`${edgeId}-case-label`}>
            <div
                className="top-6 z-10 flex items-center rounded-md border-2 border-stroke-neutral-tertiary bg-white p-1 text-xs font-medium shadow-sm hover:border-stroke-brand-secondary-hover"
                style={{
                    pointerEvents: 'all',
                    position: 'absolute',
                    transform: `translate(${labelPosition.x}px, ${labelPosition.y}px) translate(-50%, -50%)`,
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
                        onClick={handleDeleteButtonClick}
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
