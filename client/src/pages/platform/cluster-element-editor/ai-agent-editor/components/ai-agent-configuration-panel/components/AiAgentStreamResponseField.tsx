import Switch from '@/components/Switch/Switch';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {twMerge} from 'tailwind-merge';

import useAiAgentStreamResponse from './hooks/useAiAgentStreamResponse';

export default function AiAgentStreamResponseField() {
    const {isStreaming, isStreamingSupported, updateStreaming} = useAiAgentStreamResponse();
    const readOnly = useWorkflowEditorReadOnly();

    if (!isStreamingSupported) {
        return null;
    }

    return (
        <fieldset className="flex flex-col border-0">
            <h2 className="mb-2">Stream response</h2>

            <label className={twMerge('flex w-fit items-start gap-2', readOnly ? 'cursor-default' : 'cursor-pointer')}>
                <Switch
                    aria-label="Stream response"
                    checked={isStreaming}
                    disabled={readOnly}
                    onCheckedChange={updateStreaming}
                />

                <span className="text-sm leading-5 font-normal text-content-neutral-secondary">
                    Send the response back token by token instead of once it is complete.
                </span>
            </label>
        </fieldset>
    );
}
