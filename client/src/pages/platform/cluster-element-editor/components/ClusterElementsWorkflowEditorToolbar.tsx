import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Panel, useReactFlow} from '@xyflow/react';
import {BrushCleaningIcon, FocusIcon, LockIcon, LockOpenIcon, ZoomInIcon, ZoomOutIcon} from 'lucide-react';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

interface ClusterElementsWorkflowEditorToolbarPropsI {
    onResetLayout: () => void;
}

const ClusterElementsWorkflowEditorToolbar = ({onResetLayout}: ClusterElementsWorkflowEditorToolbarPropsI) => {
    const {nodesLocked, setNodesLocked} = useClusterElementsDataStore(
        useShallow((state) => ({
            nodesLocked: state.nodesLocked,
            setNodesLocked: state.setNodesLocked,
        }))
    );

    const {fitView, zoomIn, zoomOut} = useReactFlow();

    const handleZoomIn = useCallback(() => zoomIn({duration: 300}), [zoomIn]);

    const handleZoomOut = useCallback(() => zoomOut({duration: 300}), [zoomOut]);

    const handleFitView = useCallback(() => {
        fitView({duration: 500, minZoom: 0.2, padding: {bottom: '16px', left: '16px', right: '16px', top: '64px'}});
    }, [fitView]);

    const handleToggleLock = useCallback(() => {
        setNodesLocked(!nodesLocked);
    }, [nodesLocked, setNodesLocked]);

    return (
        <Panel className="m-2 mb-3" position="bottom-left">
            <ButtonGroup>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            aria-label="Zoom in"
                            icon={<ZoomInIcon />}
                            onClick={handleZoomIn}
                            size="icon"
                            variant="outline"
                        />
                    </TooltipTrigger>

                    <TooltipContent className="rounded-lg bg-surface-tooltip text-content-ontooltip" side="top">
                        Zoom in
                    </TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            aria-label="Zoom out"
                            icon={<ZoomOutIcon />}
                            onClick={handleZoomOut}
                            size="icon"
                            variant="outline"
                        />
                    </TooltipTrigger>

                    <TooltipContent className="rounded-lg bg-surface-tooltip text-content-ontooltip" side="top">
                        Zoom out
                    </TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            aria-label="Fit to screen"
                            icon={<FocusIcon />}
                            onClick={handleFitView}
                            size="icon"
                            variant="outline"
                        />
                    </TooltipTrigger>

                    <TooltipContent className="rounded-lg bg-surface-tooltip text-content-ontooltip" side="top">
                        Fit to screen
                    </TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            aria-label="Reset layout"
                            icon={<BrushCleaningIcon />}
                            onClick={onResetLayout}
                            size="icon"
                            variant="outline"
                        />
                    </TooltipTrigger>

                    <TooltipContent className="rounded-lg bg-surface-tooltip text-content-ontooltip" side="top">
                        Reset layout
                    </TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            aria-label={nodesLocked ? 'Unlock node movement' : 'Lock node movement'}
                            icon={nodesLocked ? <LockIcon /> : <LockOpenIcon />}
                            onClick={handleToggleLock}
                            size="icon"
                            variant="outline"
                        />
                    </TooltipTrigger>

                    <TooltipContent className="rounded-lg bg-surface-tooltip text-content-ontooltip" side="top">
                        {nodesLocked ? 'Unlock node movement' : 'Lock node movement'}
                    </TooltipContent>
                </Tooltip>
            </ButtonGroup>
        </Panel>
    );
};

export default ClusterElementsWorkflowEditorToolbar;
