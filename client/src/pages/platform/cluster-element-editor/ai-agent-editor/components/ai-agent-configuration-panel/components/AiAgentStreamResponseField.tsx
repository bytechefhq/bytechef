import Switch from '@/components/Switch/Switch';

import useAiAgentStreamResponse from './hooks/useAiAgentStreamResponse';

export default function AiAgentStreamResponseField() {
    const {isStreaming, isStreamingSupported, updateStreaming} = useAiAgentStreamResponse();

    if (!isStreamingSupported) {
        return null;
    }

    return (
        <fieldset className="flex flex-col border-0">
            <h2 className="mb-2">Stream response</h2>

            <label className="flex w-fit cursor-pointer items-start gap-2">
                <Switch aria-label="Stream response" checked={isStreaming} onCheckedChange={updateStreaming} />

                <span className="text-sm leading-5 font-normal text-content-neutral-secondary">
                    Send the response back token by token instead of once it is complete.
                </span>
            </label>
        </fieldset>
    );
}
