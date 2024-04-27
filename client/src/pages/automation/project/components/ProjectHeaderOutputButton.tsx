import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {SquareChevronRightIcon} from 'lucide-react';
import {RefObject} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';

const ProjectHeaderOutputButton = ({
    bottomResizablePanelRef,
}: {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
}) => {
    const {setShowBottomPanelOpen, showBottomPanelOpen} = useWorkflowEditorStore();

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-gray-200"
                    onClick={() => {
                        setShowBottomPanelOpen(!showBottomPanelOpen);

                        if (bottomResizablePanelRef.current) {
                            bottomResizablePanelRef.current.resize(!showBottomPanelOpen ? 35 : 0);
                        }
                    }}
                    size="icon"
                    variant="ghost"
                >
                    <SquareChevronRightIcon className="h-5" />
                </Button>
            </TooltipTrigger>

            <TooltipContent>Show the current workflow test execution output</TooltipContent>
        </Tooltip>
    );
};

export default ProjectHeaderOutputButton;
