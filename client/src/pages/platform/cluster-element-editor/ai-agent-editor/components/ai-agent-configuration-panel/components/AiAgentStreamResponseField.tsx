import Switch from '@/components/Switch/Switch';

import useAiAgentStreamResponse from './hooks/useAiAgentStreamResponse';

export default function AiAgentStreamResponseField() {
    const {isStreaming, isStreamingSupported, updateStreaming} = useAiAgentStreamResponse();

    if (!isStreamingSupported) {
        return null;
    }

    return (
        <Switch
            checked={isStreaming}
            description="Send the response back token by token instead of once it is complete."
            label="Stream response"
            onCheckedChange={updateStreaming}
        />
    );
}
