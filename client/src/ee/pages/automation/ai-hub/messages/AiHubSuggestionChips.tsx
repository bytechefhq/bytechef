import {Button} from '@/components/ui/button';
import {useAui} from '@assistant-ui/react';
import {FC} from 'react';
import {twMerge} from 'tailwind-merge';

// Sample questions surfaced on the empty chat states (home panel + empty thread). Each one maps to a
// capability the BUILD agent actually has today — workflow building with a data-table target, the
// research subagent with createAssetFile persistence, typed data-table creation with seed rows,
// createKnowledgeBase + addKnowledgeBaseDocument, and the personal_agent_manager's schedule tool —
// so a click always lands on a runnable path rather than a "not supported yet" reply.
//
// Each stays at or under ~85 characters so the pill renders on ONE row inside the home panel's max-w-2xl
// column. That ceiling is measured, not estimated: at text-sm in this column the 85-character
// knowledge-base line below renders on a single row, while the phrasings that used to run 92-105
// characters wrapped to a second line and left the stack of chips looking ragged. Keep new entries under
// it — but do use the room, since the wording is also the prompt the agent receives.
const SUGGESTED_QUESTIONS: string[] = [
    'Build a lead enrichment workflow that scores inbound signups into a data table',
    'Research our top 5 competitors and save a battle card for each one',
    'Create a contacts data table with name, email, and company columns and sample rows',
    'Create a knowledge base for our product docs and add a getting-started document to it',
    "Set up a personal agent that summarizes yesterday's executions every morning",
];

interface AiHubSuggestionChipsProps {
    className?: string;
}

// Clickable sample-question chips. Clicking appends the question as a user message on the assistant-ui
// thread runtime — the same path the composer uses — so AiHubRuntimeProvider.onNew fires, auto-creates
// the task when needed, and streams the turn. Must render inside AssistantRuntimeProvider.
const AiHubSuggestionChips: FC<AiHubSuggestionChipsProps> = ({className}) => {
    const aui = useAui();

    return (
        <div className={twMerge('flex w-full flex-wrap items-center justify-center gap-2 px-4', className)}>
            {SUGGESTED_QUESTIONS.map((question) => (
                <div className="animate-in duration-200 fill-mode-both fade-in slide-in-from-bottom-2" key={question}>
                    <Button
                        // max-w-full + ellipsis rather than wrapping: on a viewport narrower than the copy budget
                        // above, a chip degrades to a truncated single row (full text still on the title tooltip)
                        // instead of reflowing to two lines and re-introducing the ragged stack.
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
                        {question}
                    </Button>
                </div>
            ))}
        </div>
    );
};

export default AiHubSuggestionChips;
