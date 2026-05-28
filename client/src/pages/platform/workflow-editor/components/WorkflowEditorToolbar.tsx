import {ButtonGroup} from '@/components/ui/button-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {NodeDataType} from '@/shared/types';
import {Panel, useReactFlow} from '@xyflow/react';
import {ArrowRightIcon, BrushCleaningIcon, FocusIcon, InfoIcon, ZoomInIcon, ZoomOutIcon} from 'lucide-react';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';

interface WorkflowEditorToolbarPropsI {
    readOnly?: boolean;
}

const WorkflowEditorToolbar = ({readOnly = false}: WorkflowEditorToolbarPropsI) => {
    const nodes = useWorkflowDataStore((state) => state.nodes);

    const {layoutDirection, setLayoutDirection} = useLayoutDirectionStore(
        useShallow((state) => ({
            layoutDirection: state.layoutDirection,
            setLayoutDirection: state.setLayoutDirection,
        }))
    );

    const setResetWorkflowLayout = useWorkflowEditorStore((state) => state.setResetWorkflowLayout);

    const {fitView, zoomIn, zoomOut} = useReactFlow();

    const taskCount = nodes.filter(
        (node) => node.type === 'workflow' && !(node.data as NodeDataType).taskDispatcher
    ).length;

    const handleZoomIn = useCallback(() => zoomIn({duration: 300}), [zoomIn]);

    const handleZoomOut = useCallback(() => zoomOut({duration: 300}), [zoomOut]);

    const handleFitView = useCallback(() => {
        fitView({duration: 500, minZoom: 0.2});
    }, [fitView]);

    const handleToggleLayout = useCallback(() => {
        setLayoutDirection(layoutDirection === 'TB' ? 'LR' : 'TB');
    }, [layoutDirection, setLayoutDirection]);

    const handleClear = useCallback(() => {
        setResetWorkflowLayout(true);
    }, [setResetWorkflowLayout]);

    return (
        <Panel className="m-2 mb-3" position="bottom-left">
            <div className="flex items-start gap-2" style={{minWidth: 'max-content', overflowX: 'auto'}}>
                <div className="flex items-center gap-1 rounded-md bg-surface-neutral-secondary px-3 py-[10px]">
                    <span className="text-xs font-medium whitespace-nowrap text-content-neutral-primary">
                        {taskCount} tasks
                    </span>

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <button className="flex cursor-default items-center rounded outline-hidden">
                                <InfoIcon className="size-4 text-content-neutral-primary" />
                            </button>
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
                            <button
                                className="flex size-9 items-center justify-center rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary hover:bg-slate-50 active:bg-surface-neutral-secondary"
                                onClick={handleZoomIn}
                            >
                                <ZoomInIcon className="size-4 text-content-neutral-primary" />
                            </button>
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
                            <button
                                className="flex size-9 items-center justify-center rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary hover:bg-slate-50 active:bg-surface-neutral-secondary"
                                onClick={handleZoomOut}
                            >
                                <ZoomOutIcon className="size-4 text-content-neutral-primary" />
                            </button>
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
                            <button
                                className="flex size-9 items-center justify-center rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary hover:bg-slate-50 active:bg-surface-neutral-secondary"
                                onClick={handleFitView}
                            >
                                <FocusIcon className="size-4 text-content-neutral-primary" />
                            </button>
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
                            <button
                                className="flex size-9 items-center justify-center rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary hover:bg-slate-50 active:bg-surface-neutral-secondary"
                                onClick={handleToggleLayout}
                            >
                                <ArrowRightIcon
                                    className="size-4 text-content-neutral-primary transition-transform duration-200"
                                    style={layoutDirection === 'LR' ? {transform: 'rotate(90deg)'} : undefined}
                                />
                            </button>
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
                            <button
                                className="flex size-9 items-center justify-center rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary hover:bg-slate-50 active:bg-surface-neutral-secondary disabled:cursor-not-allowed disabled:opacity-50"
                                disabled={readOnly}
                                onClick={handleClear}
                            >
                                <BrushCleaningIcon className="size-4 text-content-neutral-primary" />
                            </button>
                        </TooltipTrigger>

                        <TooltipContent
                            className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                            side="top"
                        >
                            Reset layout
                        </TooltipContent>
                    </Tooltip>
                </ButtonGroup>
            </div>
        </Panel>
    );
};

export default WorkflowEditorToolbar;
