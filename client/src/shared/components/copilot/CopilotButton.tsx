import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useOpenCopilot from '@/shared/components/copilot/hooks/useOpenCopilot';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon} from 'lucide-react';

export interface CopilotButtonProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: Record<string, any>;
    source: Source;
}

const CopilotButton = ({parameters = {}, source}: CopilotButtonProps) => {
    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);
    const openCopilot = useOpenCopilot();

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    if (!copilotEnabled || !ff_1570) {
        return null;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label="Ask Copilot"
                    className="[&_svg]:size-5"
                    icon={<SparklesIcon />}
                    onClick={() => openCopilot({parameters, source})}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>Open Copilot panel</TooltipContent>
        </Tooltip>
    );
};

export default CopilotButton;
