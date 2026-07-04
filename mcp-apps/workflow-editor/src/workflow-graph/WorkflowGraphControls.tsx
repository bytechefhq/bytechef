import {Panel, useReactFlow} from '@xyflow/react';
import {ArrowLeftRightIcon, ArrowUpDownIcon, MaximizeIcon, MinusIcon, PlusIcon} from 'lucide-react';

import {LayoutDirectionType} from './layout/constants';

interface WorkflowGraphControlsProps {
    direction: LayoutDirectionType;
    onToggleDirection: () => void;
}

// Zoom/pan + layout-direction controls for the read-only workflow graph, grouped bottom-left.
// Drives ReactFlow's own viewport; must render inside a ReactFlowProvider.
export default function WorkflowGraphControls({direction, onToggleDirection}: WorkflowGraphControlsProps) {
    const {fitView, zoomIn, zoomOut} = useReactFlow();

    return (
        <Panel position="bottom-left">
            <div className="flex flex-col items-center justify-center rounded-md border border-stroke-neutral-secondary bg-white">
                <button
                    aria-label={direction === 'LR' ? 'Switch to vertical layout' : 'Switch to horizontal layout'}
                    className="p-2 hover:bg-surface-neutral-primary-hover"
                    onClick={onToggleDirection}
                    type="button"
                >
                    {direction === 'LR' ? <ArrowUpDownIcon size={12} /> : <ArrowLeftRightIcon size={12} />}
                </button>

                <div className="h-px w-full bg-stroke-neutral-secondary" />

                <button
                    className="p-2 hover:bg-surface-neutral-primary-hover"
                    onClick={() => zoomIn({duration: 300})}
                    type="button"
                >
                    <PlusIcon size={12} />
                </button>

                <div className="h-px w-full bg-stroke-neutral-secondary" />

                <button
                    className="p-2 hover:bg-surface-neutral-primary-hover"
                    onClick={() => zoomOut({duration: 300})}
                    type="button"
                >
                    <MinusIcon size={12} />
                </button>

                <div className="h-px w-full bg-stroke-neutral-secondary" />

                <button
                    className="p-2 hover:bg-surface-neutral-primary-hover"
                    onClick={() => fitView({duration: 500})}
                    type="button"
                >
                    <MaximizeIcon size={12} />
                </button>
            </div>
        </Panel>
    );
}
