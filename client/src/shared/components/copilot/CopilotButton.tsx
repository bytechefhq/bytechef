import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

export interface CopilotButtonProps {
    source: Source;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: Record<string, any>;
}

const CopilotButton = ({parameters = {}, source}: CopilotButtonProps) => {
    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);
    const {copilotPanelOpen, setCopilotPanelOpen} = useCopilotPanelStore(
        useShallow((state) => ({
            copilotPanelOpen: state.copilotPanelOpen,
            setCopilotPanelOpen: state.setCopilotPanelOpen,
        }))
    );

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const handleClick = () => {
        const currentContext = useCopilotStore.getState().context;

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters,
            source,
        });

        if (!copilotPanelOpen) {
            setCopilotPanelOpen(!copilotPanelOpen);
        }
    };

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                {ai.copilot.enabled && ff_1570 && (
                    <Button
                        className="[&_svg]:size-5"
                        icon={<SparklesIcon />}
                        onClick={handleClick}
                        size="icon"
                        variant="ghost"
                    />
                )}
            </TooltipTrigger>

            <TooltipContent>Open Copilot panel</TooltipContent>
        </Tooltip>
    );
};

export default CopilotButton;
