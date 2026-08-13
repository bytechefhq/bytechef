import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useOpenCopilot from '@/shared/components/copilot/hooks/useOpenCopilot';
import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {SparklesIcon} from 'lucide-react';

export interface CopilotButtonProps {
    mode?: MODE;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: Record<string, any>;
    source: Source;
}

const CopilotButton = ({mode = MODE.ASK, parameters = {}, source}: CopilotButtonProps) => {
    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);
    const openCopilot = useOpenCopilot();

    if (!copilotEnabled) {
        return null;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label="Ask Copilot"
                    className="[&_svg]:size-5"
                    icon={<SparklesIcon />}
                    onClick={() => openCopilot({mode, parameters, source})}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>Open Copilot panel</TooltipContent>
        </Tooltip>
    );
};

export default CopilotButton;
