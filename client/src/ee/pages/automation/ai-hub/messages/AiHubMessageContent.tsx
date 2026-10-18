import {MarkdownText} from '@/components/assistant-ui/markdown-text';
import {AiHubToolCallFallback} from '@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer';
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
import {MessagePrimitive, type SourceMessagePartComponent} from '@assistant-ui/react';
import {ExternalLinkIcon} from 'lucide-react';

const SourceComponent: SourceMessagePartComponent = ({title, url}) => {
    if (!url) {
        return null;
    }

    const linkLabel = title && title.length > 0 ? title : url;

    return (
        <a
            className="my-1 inline-flex items-center gap-1 rounded-md border border-border bg-muted/50 px-2 py-0.5 text-xs text-muted-foreground hover:bg-muted hover:text-foreground"
            href={url}
            rel="noreferrer"
            target="_blank"
        >
            <ExternalLinkIcon className="size-3" />

            <span className="max-w-[18rem] truncate">{linkLabel}</span>
        </a>
    );
};

const AiHubMessageContent = () => {
    return (
        <MessagePrimitive.Parts
            components={{
                Source: SourceComponent,
                Text: MarkdownText,
                data: {
                    by_name: aiChatDataComponents,
                },
                tools: {
                    Fallback: AiHubToolCallFallback,
                },
            }}
        />
    );
};

export default AiHubMessageContent;
