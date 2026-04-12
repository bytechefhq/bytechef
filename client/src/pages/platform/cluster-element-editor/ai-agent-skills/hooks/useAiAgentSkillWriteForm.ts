import {useAiAgentSkillsStore} from '@/pages/platform/cluster-element-editor/ai-agent-skills/stores/useAiAgentSkillsStore';
import {useCreateAiAgentSkillFromInstructionsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';

export default function useAiAgentSkillWriteForm() {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [instructions, setInstructions] = useState('');

    const {setSkillsView} = useAiAgentSkillsStore();

    const queryClient = useQueryClient();

    const createSkillFromInstructionsMutation = useCreateAiAgentSkillFromInstructionsMutation({
        onError: (error: Error) => {
            toast.error('Failed to create skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiAgentSkills']});

            setName('');
            setDescription('');
            setInstructions('');
            setSkillsView('list');
        },
    });

    const handleCreateFromInstructions = useCallback(() => {
        if (!name.trim() || !instructions.trim()) {
            toast.error('Name and instructions are required');

            return;
        }

        createSkillFromInstructionsMutation.mutate({
            description: description.trim() || undefined,
            instructions: instructions.trim(),
            name: name.trim(),
        });
    }, [createSkillFromInstructionsMutation, description, instructions, name]);

    return {
        createSkillFromInstructionsMutation,
        description,
        handleCreateFromInstructions,
        instructions,
        name,
        setDescription,
        setInstructions,
        setName,
    };
}
