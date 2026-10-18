import {Button} from '@/components/ui/button';
import {useAui} from '@assistant-ui/react';
import {FC} from 'react';
import {twMerge} from 'tailwind-merge';

/**
 * Sample questions for the empty chat states. Each maps to something the hub's agent can do on this line (read
 * projects and workflows, build a workflow), and each stays short enough to render on one row.
 */
const SUGGESTED_QUESTIONS: string[] = [
    'Build a workflow that posts new GitHub issues to a Slack channel',
    'Which of my projects have workflows that are not deployed yet?',
    'Explain what my most recently updated workflow does',
    'Build a scheduled workflow that emails me a daily summary',
];

interface AiHubSuggestionChipsProps {
    className?: string;
}

/**
 * Clickable sample-question chips. A click appends the question as a user message on the assistant-ui thread, the
 * same path the composer takes, so the runtime creates the chat when needed and streams the turn. Must render inside
 * the runtime provider.
 */
const AiHubSuggestionChips: FC<AiHubSuggestionChipsProps> = ({className}) => {
    const aui = useAui();

    return (
        <div className={twMerge('flex w-full flex-wrap items-center justify-center gap-2 px-4', className)}>
            {SUGGESTED_QUESTIONS.map((question) => (
                <div
                    className="max-w-full min-w-0 animate-in duration-200 fill-mode-both fade-in slide-in-from-bottom-2"
                    key={question}
                >
                    <Button
                        className="h-auto max-w-full overflow-hidden rounded-full border border-border/60 px-3.5 py-1.5 text-sm font-normal text-ellipsis whitespace-nowrap text-foreground transition-colors hover:bg-muted"
                        onClick={() =>
                            aui.thread.append({
                                content: [{text: question, type: 'text'}],
                                role: 'user',
                            })
                        }
                        title={question}
                        variant="ghost"
                    >
                        <span className="min-w-0 truncate">{question}</span>
                    </Button>
                </div>
            ))}
        </div>
    );
};

export default AiHubSuggestionChips;
