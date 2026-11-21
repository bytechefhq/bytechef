import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import MarkdownEditor from '@/shared/components/markdown-editor/MarkdownEditor';
import {useUpdateAiAgentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useRef, useState} from 'react';
import {toast} from 'sonner';

interface AgentInstructionsCardProps {
    agentId: string;
    instructions: string | null;
}

const AgentInstructionsCard = ({agentId, instructions}: AgentInstructionsCardProps) => {
    const [value, setValue] = useState(instructions ?? '');

    const dirtyRef = useRef(false);

    const queryClient = useQueryClient();

    const updateAgentMutation = useUpdateAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to save instructions.');
        },
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const handleBlur = (markdown: string) => {
        if (!dirtyRef.current) {
            return;
        }

        dirtyRef.current = false;

        updateAgentMutation.mutate({input: {id: agentId, instructions: markdown}});
    };

    const handleChange = (markdown: string) => {
        dirtyRef.current = true;

        setValue(markdown);
    };

    return (
        <AgentSection title="Instructions">
            <fieldset className="border-0 p-0">
                <MarkdownEditor
                    ariaLabel="Instructions"
                    className="min-h-32"
                    onBlur={handleBlur}
                    onChange={handleChange}
                    placeholder="Describe how this agent should behave…"
                    value={value}
                />
            </fieldset>
        </AgentSection>
    );
};

export default AgentInstructionsCard;
