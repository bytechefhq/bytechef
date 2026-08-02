import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import GraphTransitionBadges from '@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionBadges';
import {LayoutDirectionType} from '@/shared/constants';
import {EdgeLabelRenderer} from '@xyflow/react';
import {CheckIcon, PenIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import useGraphNodeLabel from './useGraphNodeLabel';

interface GraphNodeLabelProps {
    edgeId: string;
    layoutDirection: LayoutDirectionType;
    nodeIndex: number;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

/**
 * Inline-editable name chip for one graph node's lane, rendered as the edge label of the lane's
 * entry edge (top-ghost -> first task, or top-ghost -> placeholder for an empty lane) — the
 * `graph` analog of `BranchCaseLabel`. Also carries the lane's transition badges
 * (`GraphTransitionBadges`), since the chip is the natural "lane header" spot for both.
 *
 * Deletion is allowed for a node with tasks (behind the same two-click confirm as branch case
 * deletion) AND for an empty node — unlike branch, graph never requires keeping a minimum
 * number of nodes.
 */
export default function GraphNodeLabel({
    edgeId,
    layoutDirection,
    nodeIndex,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: GraphNodeLabelProps) {
    const {
        dangling,
        dynamic,
        handleAddNodeClick,
        handleDeleteButtonClick,
        handleEditNameClick,
        handleSaveNameClick,
        inputRef,
        isDeleteConfirmationVisible,
        isLastNode,
        isNameEditable,
        labelPosition,
        nameError,
        nameValue,
        setNameValue,
        targets,
    } = useGraphNodeLabel({
        edgeId,
        layoutDirection,
        nodeIndex,
        sourceX,
        sourceY,
        targetX,
        targetY,
    });

    // TB centers the chip on its lane column; LR anchors the chip's right edge past the split
    // bar with the chip above (or below, for the row under the dispatcher's label text) its row
    // line — the anchor comes from computeBranchCaseLabelPosition (shared, purely geometric).
    const selfAnchorByPlacement: Record<typeof labelPosition.anchor, string> = {
        above: 'translate(-100%, -100%)',
        below: 'translate(-100%, 0%)',
        center: 'translate(-50%, -50%)',
    };
    const selfAnchor = selfAnchorByPlacement[labelPosition.anchor];

    return (
        <EdgeLabelRenderer key={`${edgeId}-graph-node-label`}>
            <div
                className={twMerge(
                    'z-10 flex flex-col items-start gap-1 rounded-md border-2 border-stroke-neutral-tertiary bg-white p-1 text-xs font-medium shadow-xs hover:border-stroke-brand-secondary-hover',
                    layoutDirection === 'LR' ? 'top-0' : 'top-6'
                )}
                style={{
                    pointerEvents: 'all',
                    position: 'absolute',
                    transform: `translate(${labelPosition.x}px, ${labelPosition.y}px) ${selfAnchor}`,
                }}
            >
                <div className="flex items-center gap-0.5">
                    <div className="group relative flex items-center">
                        <Input
                            aria-invalid={!!nameError}
                            // field-sizing-content hugs the name instead of holding a fixed
                            // width, which otherwise leaves dead space between a short name and
                            // the pen pinned to the input's right edge
                            className="field-sizing-content h-auto max-w-24 border-none pr-8 text-xs shadow-none group-hover:bg-surface-brand-secondary disabled:cursor-auto disabled:text-xs disabled:opacity-100 md:text-xs"
                            disabled={!isNameEditable}
                            onChange={(event) => setNameValue(event.target.value)}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter' && isNameEditable) {
                                    handleSaveNameClick();
                                }
                            }}
                            ref={inputRef}
                            value={nameValue}
                        />

                        <Button
                            className="absolute top-1/2 right-1 size-4 -translate-y-1/2 cursor-pointer text-content-neutral-primary/50 hover:bg-transparent hover:text-content-neutral-primary [&_svg]:size-3"
                            icon={isNameEditable ? <CheckIcon className="text-content-brand-primary" /> : <PenIcon />}
                            onClick={isNameEditable ? handleSaveNameClick : handleEditNameClick}
                            size="icon"
                            variant="ghost"
                        />
                    </div>

                    {/* Directly after the lane's own name, so the header reads "node_0 -> node_3"
                        as one phrase. Anything placed between the two would interrupt it, which
                        is why the buttons below follow rather than precede it. */}

                    <GraphTransitionBadges dangling={dangling} dynamic={dynamic} targets={targets} />

                    {isLastNode && (
                        <Button
                            className="cursor-pointer text-content-neutral-primary/50 hover:bg-surface-neutral-primary-hover hover:text-content-neutral-primary"
                            icon={<PlusIcon />}
                            onClick={handleAddNodeClick}
                            size="iconXs"
                            variant="ghost"
                        />
                    )}

                    {/* Destructive action last */}

                    <Button
                        className="cursor-pointer text-content-destructive/50 hover:bg-surface-destructive-secondary hover:text-content-destructive"
                        icon={isDeleteConfirmationVisible ? <CheckIcon /> : <TrashIcon />}
                        onClick={handleDeleteButtonClick}
                        size="iconXs"
                        variant="ghost"
                    />
                </div>

                {nameError && <span className="px-1 text-xs text-content-destructive">{nameError}</span>}
            </div>
        </EdgeLabelRenderer>
    );
}
