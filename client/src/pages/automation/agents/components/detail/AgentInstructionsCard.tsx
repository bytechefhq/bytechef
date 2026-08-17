import {Textarea} from '@/components/ui/textarea';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useUpdateAiAgentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

interface AgentInstructionsCardProps {
    agentId: string;
    instructions: string | null;
}

const AgentInstructionsCard = ({agentId, instructions}: AgentInstructionsCardProps) => {
    const [value, setValue] = useState(instructions ?? '');

    const queryClient = useQueryClient();

    const updateAgentMutation = useUpdateAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to save instructions.');
        },
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const handleBlur = () => {
        if (value === (instructions ?? '')) {
            return;
        }

        updateAgentMutation.mutate({input: {id: agentId, instructions: value}});
    };

    return (
        <AgentSection title="Instructions">
            <fieldset className="border-0 p-0">
                <Textarea
                    className="min-h-32"
                    onBlur={handleBlur}
                    onChange={(event) => setValue(event.target.value)}
                    placeholder="Describe how this agent should behave…"
                    value={value}
                />
            </fieldset>
        </AgentSection>
    );
};

export default AgentInstructionsCard;
