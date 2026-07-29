import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {NodeDataType} from '@/shared/types';
import {Panel, useReactFlow} from '@xyflow/react';
import {
    ArrowRightIcon,
    BrushCleaningIcon,
    FocusIcon,
    InfoIcon,
    LockIcon,
    LockOpenIcon,
    NetworkIcon,
    RedoIcon,
    StickyNoteIcon,
    UndoIcon,
    ZoomInIcon,
    ZoomOutIcon,
} from 'lucide-react';
import {useCallback} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowUndoRedo from '../hooks/useWorkflowUndoRedo';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import isElkLayoutSupported from '../utils/isElkLayoutSupported';

interface WorkflowEditorToolbarPropsI {
    enableUndoRedo?: boolean;
    onAddStickyNote?: () => void;
    readOnly?: boolean;
}

const WorkflowEditorToolbar = ({
    enableUndoRedo = false,
    onAddStickyNote,
    readOnly = false,
}: WorkflowEditorToolbarPropsI) => {
    const nodes = useWorkflowDataStore((state) => state.nodes);

    const {layoutDirection, setLayoutDirection} = useLayoutDirectionStore(
        useShallow((state) => ({
            layoutDirection: state.layoutDirection,
            setLayoutDirection: state.setLayoutDirection,
        }))
    );

    const {layoutEngine, setLayoutEngine} = useLayoutEngineStore(
        useShallow((state) => ({
            layoutEngine: state.layoutEngine,
            setLayoutEngine: state.setLayoutEngine,
        }))
    );

    const {nodesLocked, setNodesLocked, setResetWorkflowLayout} = useWorkflowEditorStore(
        useShallow((state) => ({
            nodesLocked: state.nodesLocked,
            setNodesLocked: state.setNodesLocked,
            setResetWorkflowLayout: state.setResetWorkflowLayout,
        }))
    );

    const {fitView, zoomIn, zoomOut} = useReactFlow();
    const {canRedo, canUndo, handleRedo, handleUndo} = useWorkflowUndoRedo();

    const taskCount = nodes.filter(
        (node) => node.type === 'workflow' && !(node.data as NodeDataType).taskDispatcher
    ).length;

    const elkLayoutSupported = isElkLayoutSupported(nodes);

    const layoutEngineLabel =
        layoutEngine === 'elk' ? 'Switch to standard layout engine' : 'Switch to experimental layout engine';

    let layoutEngineTooltip = layoutEngineLabel;

    if (!elkLayoutSupported) {
        layoutEngineTooltip = 'Experimental layout does not support this workflow yet';
    }

    const handleZoomIn = useCallback(() => zoomIn({duration: 300}), [zoomIn]);

    const handleZoomOut = useCallback(() => zoomOut({duration: 300}), [zoomOut]);

    const handleFitView = useCallback(() => {
        fitView({duration: 500, minZoom: 0.2});
    }, [fitView]);

    const handleToggleLayout = useCallback(() => {
        setLayoutDirection(layoutDirection === 'TB' ? 'LR' : 'TB');
    }, [layoutDirection, setLayoutDirection]);

    const handleToggleLayoutEngine = useCallback(() => {
        setLayoutEngine(layoutEngine === 'elk' ? 'dagre' : 'elk');
    }, [layoutEngine, setLayoutEngine]);

    const handleClear = useCallback(() => {
        setResetWorkflowLayout(true);
    }, [setResetWorkflowLayout]);

    const handleToggleLock = useCallback(() => {
        setNodesLocked(!nodesLocked);
    }, [nodesLocked, setNodesLocked]);

    return (
        <>
            <Panel className="m-2" position="top-left">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span tabIndex={!elkLayoutSupported ? 0 : undefined}>
                            <Button
                                aria-label={layoutEngineLabel}
                                className={twMerge(
                                    layoutEngine === 'elk' && elkLayoutSupported && 'text-content-brand-primary'
                                )}
                                disabled={!elkLayoutSupported}
                                icon={<NetworkIcon />}
                                onClick={handleToggleLayoutEngine}
                                size="icon"
                                variant="outline"
                            />
                        </span>
                    </TooltipTrigger>

                    <TooltipContent
                        className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                        side="bottom"
                    >
                        {layoutEngineTooltip}
                    </TooltipContent>
                </Tooltip>
            </Panel>

            <Panel className="m-2 mb-3" position="bottom-left">
                <div className="flex items-start gap-2" style={{minWidth: 'max-content', overflowX: 'auto'}}>
                    <div className="flex items-center gap-1 rounded-md bg-surface-neutral-secondary px-3 py-[10px]">
                        <span className="text-xs font-medium whitespace-nowrap text-content-neutral-primary">
                            {taskCount} tasks
                        </span>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    className="size-auto cursor-default rounded p-0 hover:bg-transparent active:bg-transparent"
                                    icon={<InfoIcon className="text-content-neutral-primary" />}
                                    size="iconXs"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent
                                className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                side="top"
                            >
                                Number of tasks executed per workflow run.
                            </TooltipContent>
                        </Tooltip>
                    </div>

                    <ButtonGroup>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button icon={<ZoomInIcon />} onClick={handleZoomIn} size="icon" variant="outline" />
                            </TooltipTrigger>

                            <TooltipContent
                                className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                side="top"
                            >
                                Zoom in
                            </TooltipContent>
                        </Tooltip>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button icon={<ZoomOutIcon />} onClick={handleZoomOut} size="icon" variant="outline" />
                            </TooltipTrigger>

                            <TooltipContent
                                className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                side="top"
                            >
                                Zoom out
                            </TooltipContent>
                        </Tooltip>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button icon={<FocusIcon />} onClick={handleFitView} size="icon" variant="outline" />
                            </TooltipTrigger>

                            <TooltipContent
                                className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                side="top"
                            >
                                Fit to screen
                            </TooltipContent>
                        </Tooltip>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    icon={
                                        <ArrowRightIcon
                                            className="text-content-neutral-primary transition-transform duration-200"
                                            style={layoutDirection === 'LR' ? {transform: 'rotate(90deg)'} : undefined}
                                        />
                                    }
                                    onClick={handleToggleLayout}
                                    size="icon"
                                    variant="outline"
                                />
                            </TooltipTrigger>

                            <TooltipContent
                                className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                side="top"
                            >
                                {layoutDirection === 'TB' ? 'Switch to horizontal layout' : 'Switch to vertical layout'}
                            </TooltipContent>
                        </Tooltip>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    disabled={readOnly}
                                    icon={<BrushCleaningIcon />}
                                    onClick={handleClear}
                                    size="icon"
                                    variant="outline"
                                />
                            </TooltipTrigger>

                            <TooltipContent
                                className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                side="top"
                            >
                                Reset layout
                            </TooltipContent>
                        </Tooltip>

                        {!readOnly && onAddStickyNote && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        icon={<StickyNoteIcon />}
                                        onClick={onAddStickyNote}
                                        size="icon"
                                        variant="outline"
                                    />
                                </TooltipTrigger>

                                <TooltipContent
                                    className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                    side="top"
                                >
                                    Add note
                                </TooltipContent>
                            </Tooltip>
                        )}

                        {!readOnly && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button
                                        aria-label={nodesLocked ? 'Unlock node movement' : 'Lock node movement'}
                                        className="flex size-9 items-center justify-center rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary hover:bg-slate-50 active:bg-surface-neutral-secondary"
                                        onClick={handleToggleLock}
                                    >
                                        {nodesLocked ? (
                                            <LockIcon className="size-4 text-content-neutral-primary" />
                                        ) : (
                                            <LockOpenIcon className="size-4 text-content-neutral-primary" />
                                        )}
                                    </button>
                                </TooltipTrigger>

                                <TooltipContent
                                    className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                    side="top"
                                >
                                    {nodesLocked ? 'Unlock node movement' : 'Lock node movement'}
                                </TooltipContent>
                            </Tooltip>
                        )}
                    </ButtonGroup>

                    {enableUndoRedo && !readOnly && (
                        <ButtonGroup>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!canUndo}
                                        icon={<UndoIcon />}
                                        onClick={handleUndo}
                                        size="icon"
                                        variant="outline"
                                    />
                                </TooltipTrigger>

                                <TooltipContent
                                    className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                    side="top"
                                >
                                    Undo
                                </TooltipContent>
                            </Tooltip>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!canRedo}
                                        icon={<RedoIcon />}
                                        onClick={handleRedo}
                                        size="icon"
                                        variant="outline"
                                    />
                                </TooltipTrigger>

                                <TooltipContent
                                    className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                    side="top"
                                >
                                    Redo
                                </TooltipContent>
                            </Tooltip>
                        </ButtonGroup>
                    )}
                </div>
            </Panel>
        </>
    );
};

export default WorkflowEditorToolbar;
