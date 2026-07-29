import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {LayoutDirectionType} from '@/shared/constants';
import {EdgeLabelRenderer} from '@xyflow/react';
import {CheckIcon, PenIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import useBranchCaseLabel from './useBranchCaseLabel';

interface BranchCaseLabelProps {
    caseKey: string | number;
    edgeId: string;
    layoutDirection: LayoutDirectionType;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

export default function BranchCaseLabel({
    caseKey,
    edgeId,
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
        layoutDirection,
        sourceX,
        sourceY,
        targetX,
        targetY,
    });

    // TB centers the chip on its case column; LR anchors the chip's right
    // edge past the split bar with the chip above (or, for the row under the
    // dispatcher's label text, below) its row line — the anchor comes from
    // computeBranchCaseLabelPosition.
    const selfAnchorByPlacement: Record<typeof labelPosition.anchor, string> = {
        above: 'translate(-100%, -100%)',
        below: 'translate(-100%, 0%)',
        center: 'translate(-50%, -50%)',
    };
    const selfAnchor = selfAnchorByPlacement[labelPosition.anchor];

    return (
        <EdgeLabelRenderer key={`${edgeId}-case-label`}>
            <div
                className={twMerge(
                    'z-10 flex items-center rounded-md border-2 border-stroke-neutral-tertiary bg-white p-1 text-xs font-medium shadow-xs hover:border-stroke-brand-secondary-hover',
                    // the TB nudge below the bar keeps chips off the bar line; in LR the
                    // chip is row-centered, so any static offset would miss the row
                    layoutDirection === 'LR' ? 'top-0' : 'top-6'
                )}
                style={{
                    pointerEvents: 'all',
                    position: 'absolute',
                    transform: `translate(${labelPosition.x}px, ${labelPosition.y}px) ${selfAnchor}`,
                }}
            >
                {isDefaultCase && <span className="p-1 text-xs">default</span>}

                {!isDefaultCase && (
                    <div className="group relative flex items-center">
                        <Input
                            className="h-auto max-w-24 border-none pr-7 text-xs shadow-none group-hover:bg-surface-brand-secondary disabled:cursor-auto disabled:text-xs disabled:opacity-100 md:text-xs"
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
                            className="absolute top-1/2 right-2 size-4 -translate-y-1/2 cursor-pointer text-content-neutral-primary/50 hover:bg-transparent hover:text-content-neutral-primary [&_svg]:size-3"
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
