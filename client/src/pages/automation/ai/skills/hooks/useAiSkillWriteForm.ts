import {useCreateAiSkillFromInstructionsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';

interface UseAiSkillWriteFormOptionsI {
    onSuccess?: () => void;
}

export default function useAiSkillWriteForm({onSuccess}: UseAiSkillWriteFormOptionsI = {}) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [instructions, setInstructions] = useState('');

    const queryClient = useQueryClient();

    const createSkillFromInstructionsMutation = useCreateAiSkillFromInstructionsMutation({
        onError: (error: Error) => {
            toast.error('Failed to create skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiSkills']});

            setName('');
            setDescription('');
            setInstructions('');

            onSuccess?.();
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
