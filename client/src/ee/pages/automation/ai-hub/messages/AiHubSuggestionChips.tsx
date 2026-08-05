import {useAui} from '@assistant-ui/react';
import {FC} from 'react';
import {twMerge} from 'tailwind-merge';

// Sample questions surfaced on the empty chat states (home panel + empty thread). Each one maps to a
// capability the BUILD agent actually has today — workflow building with a data-table target, the
// research subagent with createAssetFile persistence, typed data-table creation with seed rows,
// createKnowledgeBase + addKnowledgeBaseDocument, and the personal_agent_manager's schedule tool —
// so a click always lands on a runnable path rather than a "not supported yet" reply.
const SUGGESTED_QUESTIONS: string[] = [
    'Build a lead enrichment workflow that scores inbound signups and writes the results to a leads data table',
    'Research our top 5 competitors and save a battle card for each one',
    'Create a contacts data table with name, email, and company columns and add three sample rows',
    'Create a knowledge base for our product docs and add a getting-started document to it',
    "Set up a personal agent that summarizes yesterday's workflow executions every morning at 9:00",
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
        <div className={twMerge('flex w-full max-w-xl flex-col items-stretch gap-2', className)}>
            {SUGGESTED_QUESTIONS.map((question) => (
                <button
                    className="rounded-lg border border-border bg-background px-3 py-2 text-left text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
                    key={question}
                    onClick={() =>
                        aui.thread.append({
                            content: [{text: question, type: 'text'}],
                            role: 'user',
                        })
                    }
                    type="button"
                >
                    {question}
                </button>
            ))}
        </div>
    );
};

export default AiHubSuggestionChips;
