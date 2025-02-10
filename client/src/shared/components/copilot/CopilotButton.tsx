import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon} from 'lucide-react';

export interface CopilotButtonProps {
    source: Source;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: Record<string, any>;
}

const CopilotButton = ({parameters = {}, source}: CopilotButtonProps) => {
    const {ai} = useApplicationInfoStore();
    const {copilotPanelOpen, setContext, setCopilotPanelOpen} = useCopilotStore();
    const {workflow} = useWorkflowDataStore();

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const handleClick = () => {
        setContext({parameters, source, workflowId: workflow.id!});

        if (!copilotPanelOpen) {
            setCopilotPanelOpen(!copilotPanelOpen);
        }
    };

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                {ai.copilot.enabled && ff_1570 && (
                    <Button className="[&_svg]:size-5" onClick={handleClick} size="icon" variant="ghost">
                        <SparklesIcon />
                    </Button>
                )}
            </TooltipTrigger>

            <TooltipContent>Open Copilot panel</TooltipContent>
        </Tooltip>
    );
};

export default CopilotButton;
