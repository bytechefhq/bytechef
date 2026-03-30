import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {useAgentEvalResultTranscriptQuery} from '@/shared/middleware/graphql';
import {AlertCircleIcon, BotIcon, Loader2Icon, UserIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

interface TranscriptMessageI {
    content: string;
    role: string;
    toolCalls?: TranscriptToolCallI[];
    turnNumber?: number;
}

interface TranscriptToolCallI {
    input?: string;
    name: string;
    output?: string;
}

interface TranscriptDataI {
    expectedOutput?: string;
    messages: TranscriptMessageI[];
}

interface TranscriptDialogProps {
    onClose: () => void;
    resultId: string;
    scenarioName: string;
}

const TranscriptDialog = ({onClose, resultId, scenarioName}: TranscriptDialogProps) => {
    const {data, error, isLoading} = useAgentEvalResultTranscriptQuery({id: resultId});

    const transcriptData = useMemo<TranscriptDataI | null>(() => {
        const transcriptString = data?.agentEvalResultTranscript;

        if (!transcriptString) {
            return null;
        }

        try {
            return JSON.parse(transcriptString) as TranscriptDataI;
        } catch {
            return null;
        }
    }, [data]);

    const groupedTurns = useMemo(() => {
        if (!transcriptData?.messages) {
            return [];
        }

        const turns: {assistantMessage?: TranscriptMessageI; turnIndex: number; userMessage?: TranscriptMessageI}[] =
            [];

        let currentTurn: {assistantMessage?: TranscriptMessageI; turnIndex: number; userMessage?: TranscriptMessageI} =
            {turnIndex: 1};

        for (const message of transcriptData.messages) {
            if (message.role === 'user') {
                if (currentTurn.userMessage) {
                    turns.push(currentTurn);

                    currentTurn = {turnIndex: turns.length + 1};
                }

                currentTurn.userMessage = message;
            } else if (message.role === 'assistant') {
                currentTurn.assistantMessage = message;

                turns.push(currentTurn);

                currentTurn = {turnIndex: turns.length + 1};
            }
        }

        if (currentTurn.userMessage || currentTurn.assistantMessage) {
            turns.push(currentTurn);
        }

        return turns;
    }, [transcriptData]);

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent className="max-h-[80vh] max-w-2xl overflow-y-auto">
                <DialogHeader className="flex flex-row items-center justify-between">
                    <div>
                        <DialogTitle>Conversation Transcript - {scenarioName}</DialogTitle>

                        <DialogDescription>Conversation transcript for this scenario result.</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                {isLoading && (
                    <div className="flex items-center justify-center py-8">
                        <Loader2Icon className="size-5 animate-spin text-gray-400" />

                        <span className="ml-2 text-sm text-gray-500">Loading transcript...</span>
                    </div>
                )}

                {!!error && (
                    <div className="flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-4 py-3">
                        <AlertCircleIcon className="size-4 text-red-500" />

                        <span className="text-sm text-red-600">Failed to load transcript.</span>
                    </div>
                )}

                {!isLoading && !error && !transcriptData && (
                    <div className="rounded-md border border-border/50 bg-gray-50 px-4 py-3">
                        <div className="text-sm text-gray-500">No transcript data available.</div>
                    </div>
                )}

                {transcriptData && (
                    <div className="space-y-4">
                        {groupedTurns.map((turn) => (
                            <div className="space-y-2" key={turn.turnIndex}>
                                {groupedTurns.length > 1 && (
                                    <div className="text-xs font-medium text-gray-400">Turn {turn.turnIndex}</div>
                                )}

                                {turn.userMessage && (
                                    <div className="rounded-lg border border-blue-100 bg-blue-50/50 px-3 py-2.5">
                                        <div className="mb-1.5 flex items-center gap-1.5">
                                            <UserIcon className="size-3.5 text-blue-600" />

                                            <span className="text-xs font-semibold text-blue-700">User</span>
                                        </div>

                                        <div className="whitespace-pre-wrap text-sm text-gray-800">
                                            {turn.userMessage.content}
                                        </div>
                                    </div>
                                )}

                                {turn.assistantMessage && (
                                    <div className="rounded-lg border border-gray-200 bg-gray-50/50 px-3 py-2.5">
                                        <div className="mb-1.5 flex items-center gap-1.5">
                                            <BotIcon className="size-3.5 text-gray-600" />

                                            <span className="text-xs font-semibold text-gray-700">Assistant</span>
                                        </div>

                                        <div className="whitespace-pre-wrap text-sm text-gray-800">
                                            {turn.assistantMessage.content}
                                        </div>

                                        {turn.assistantMessage.toolCalls &&
                                            turn.assistantMessage.toolCalls.length > 0 && (
                                                <div className="mt-2 space-y-1.5">
                                                    {turn.assistantMessage.toolCalls.map((toolCall, toolCallIndex) => (
                                                        <details
                                                            className="rounded border border-gray-200 bg-white"
                                                            key={toolCallIndex}
                                                        >
                                                            <summary
                                                                className={twMerge(
                                                                    'cursor-pointer px-2.5 py-1.5 text-xs font-medium text-gray-600',
                                                                    'hover:text-gray-800'
                                                                )}
                                                            >
                                                                Tool: {toolCall.name}
                                                            </summary>

                                                            <div className="space-y-1 border-t border-gray-100 px-2.5 py-2">
                                                                {toolCall.input && (
                                                                    <div>
                                                                        <div className="text-[10px] font-medium uppercase tracking-wide text-gray-400">
                                                                            Input
                                                                        </div>

                                                                        <pre className="mt-0.5 overflow-x-auto whitespace-pre-wrap break-all rounded bg-gray-100 p-1.5 font-mono text-xs text-gray-700">
                                                                            {toolCall.input}
                                                                        </pre>
                                                                    </div>
                                                                )}

                                                                {toolCall.output && (
                                                                    <div>
                                                                        <div className="text-[10px] font-medium uppercase tracking-wide text-gray-400">
                                                                            Output
                                                                        </div>

                                                                        <pre className="mt-0.5 overflow-x-auto whitespace-pre-wrap break-all rounded bg-gray-100 p-1.5 font-mono text-xs text-gray-700">
                                                                            {toolCall.output}
                                                                        </pre>
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </details>
                                                    ))}
                                                </div>
                                            )}
                                    </div>
                                )}
                            </div>
                        ))}

                        {transcriptData.expectedOutput && (
                            <div className="rounded-lg border border-amber-200 bg-amber-50/50 px-3 py-2.5">
                                <div className="mb-1 text-xs font-semibold text-amber-700">Expected Output</div>

                                <div className="whitespace-pre-wrap text-sm text-gray-700">
                                    {transcriptData.expectedOutput}
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default TranscriptDialog;
