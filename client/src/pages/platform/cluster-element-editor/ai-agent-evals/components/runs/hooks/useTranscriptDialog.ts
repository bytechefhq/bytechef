import {useAgentEvalResultTranscriptQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

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

interface GroupedTurnI {
    assistantMessage?: TranscriptMessageI;
    turnIndex: number;
    userMessage?: TranscriptMessageI;
}

export default function useTranscriptDialog(resultId: string) {
    const {data, error, isLoading} = useAgentEvalResultTranscriptQuery({id: resultId});

    const transcriptData = useMemo<TranscriptDataI | null>(() => {
        const transcriptString = data?.agentEvalResultTranscript;

        if (!transcriptString) {
            return null;
        }

        try {
            return JSON.parse(transcriptString) as TranscriptDataI;
        } catch (parseError) {
            console.error('Failed to parse transcript JSON:', parseError);

            return null;
        }
    }, [data]);

    const groupedTurns = useMemo<GroupedTurnI[]>(() => {
        if (!transcriptData?.messages) {
            return [];
        }

        const turns: GroupedTurnI[] = [];

        let currentTurn: GroupedTurnI = {turnIndex: 1};

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

    return {
        error,
        groupedTurns,
        isLoading,
        transcriptData,
    };
}

export type {GroupedTurnI, TranscriptDataI, TranscriptMessageI, TranscriptToolCallI};
