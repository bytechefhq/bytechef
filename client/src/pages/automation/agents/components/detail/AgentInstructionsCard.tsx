import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import MarkdownEditor from '@/shared/components/markdown-editor/MarkdownEditor';
import {useUpdateAiAgentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

interface AgentInstructionsCardProps {
    agentId: string;
    externalChangeRevision: number;
    instructions: string | null;
}

const AgentInstructionsCard = ({agentId, externalChangeRevision, instructions}: AgentInstructionsCardProps) => {
    const [dirty, setDirty] = useState(false);
    const [syncedRevision, setSyncedRevision] = useState(externalChangeRevision);
    const [value, setValue] = useState(instructions ?? '');

    if (syncedRevision !== externalChangeRevision) {
        setSyncedRevision(externalChangeRevision);

        if (!dirty) {
            setValue(instructions ?? '');
        }
    }

    const queryClient = useQueryClient();

    const updateAgentMutation = useUpdateAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to save instructions.');
        },
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const handleBlur = (markdown: string) => {
        if (!dirty) {
            return;
        }

        setDirty(false);

        updateAgentMutation.mutate({input: {id: agentId, instructions: markdown}});
    };

    const handleChange = (markdown: string) => {
        setDirty(true);
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
