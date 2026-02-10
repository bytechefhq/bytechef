import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {SquareChevronRightIcon} from 'lucide-react';
import {RefObject} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useShallow} from 'zustand/react/shallow';

const IntegrationHeaderOutputButton = ({
    bottomResizablePanelRef,
}: {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle>;
}) => {
    const {setShowBottomPanelOpen, showBottomPanel} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowBottomPanelOpen: state.setShowBottomPanelOpen,
            showBottomPanel: state.showBottomPanel,
        }))
    );

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-background/70 [&_svg]:size-5"
                    icon={<SquareChevronRightIcon />}
                    onClick={() => {
                        setShowBottomPanelOpen(!showBottomPanel);

                        if (bottomResizablePanelRef.current) {
                            bottomResizablePanelRef.current.resize(!showBottomPanel ? 35 : 0);
                        }
                    }}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>Show the current workflow test execution output</TooltipContent>
        </Tooltip>
    );
};

export default IntegrationHeaderOutputButton;
