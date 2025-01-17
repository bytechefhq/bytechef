import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useCopilotStore} from '@/pages/platform/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon} from 'lucide-react';

const ProjectHeaderCopilotButton = () => {
    const {ai} = useApplicationInfoStore();
    const {copilotPanelOpen, setCopilotPanelOpen} = useCopilotStore();

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                {ai.copilot.enabled && ff_1570 && (
                    <Button
                        onClick={() => !copilotPanelOpen && setCopilotPanelOpen(!copilotPanelOpen)}
                        size="icon"
                        variant="ghost"
                    >
                        <SparklesIcon className="h-5" />
                    </Button>
                )}
            </TooltipTrigger>

            <TooltipContent>Open Copilot panel</TooltipContent>
        </Tooltip>
    );
};

export default ProjectHeaderCopilotButton;
