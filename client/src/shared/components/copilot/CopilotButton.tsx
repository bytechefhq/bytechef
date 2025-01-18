import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon} from 'lucide-react';
import {useEffect} from 'react';

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

    useEffect(() => {
        setContext({parameters, source, workflowId: workflow.id!});

        return () => {
            setContext(undefined);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [source, workflow]);

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                {ai.copilot.enabled && ff_1570 && (
                    <Button
                        className="[&_svg]:size-5"
                        onClick={() => !copilotPanelOpen && setCopilotPanelOpen(!copilotPanelOpen)}
                        size="icon"
                        variant="ghost"
                    >
                        <SparklesIcon />
                    </Button>
                )}
            </TooltipTrigger>

            <TooltipContent>Open Copilot panel</TooltipContent>
        </Tooltip>
    );
};

export default CopilotButton;
