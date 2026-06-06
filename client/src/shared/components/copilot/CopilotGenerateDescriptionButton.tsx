import Button from '@/components/Button/Button';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Loader2Icon, SparklesIcon} from 'lucide-react';

import {useGenerateWorkflowDescription} from './useGenerateWorkflowDescription';

interface CopilotGenerateDescriptionButtonPropsI {
    environmentId: number;
    onApply: (value: string) => void;
    workflowId?: string;
    workflowNodeName?: string;
}

const CopilotGenerateDescriptionButton = ({
    environmentId,
    onApply,
    workflowId,
    workflowNodeName,
}: CopilotGenerateDescriptionButtonPropsI) => {
    const ai = useApplicationInfoStore((state) => state.ai);
    const ff1570 = useFeatureFlagsStore()('ff-1570');

    const {generate, isPending} = useGenerateWorkflowDescription();

    if (!ai.copilot.enabled || !ff1570 || !workflowId) {
        return null;
    }

    const handleGenerate = async () => {
        try {
            const result = await generate({environmentId, workflowId, workflowNodeName});

            onApply(result.value);
        } catch {
            // The error is already surfaced via the global fetch-interceptor toast.
        }
    };

    return (
        <Button
            aria-label="Generate with AI"
            disabled={isPending}
            icon={isPending ? <Loader2Icon className="animate-spin" /> : <SparklesIcon />}
            onClick={handleGenerate}
            size="iconXs"
            variant="ghost"
        />
    );
};

export default CopilotGenerateDescriptionButton;
