import downloadAiAgentSkill from '@/pages/platform/cluster-element-editor/ai-agent-skills/utils/downloadAiAgentSkill';
import {AiAgentSkill, useDeleteAiAgentSkillMutation, useUpdateAiAgentSkillMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';
import {toast} from 'sonner';

export default function useAiAgentSkillsList(skills: AiAgentSkill[]) {
    const [searchQuery, setSearchQuery] = useState('');

    const queryClient = useQueryClient();

    const filteredSkills = useMemo(
        () => skills.filter((skill) => skill.name.toLowerCase().includes(searchQuery.toLowerCase())),
        [searchQuery, skills]
    );

    const deleteAiAgentSkillMutation = useDeleteAiAgentSkillMutation({
        onError: (error: Error) => {
            toast.error('Failed to delete skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiAgentSkills']});
        },
    });

    const updateAiAgentSkillMutation = useUpdateAiAgentSkillMutation({
        onError: (error: Error) => {
            toast.error('Failed to rename skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiAgentSkills']});
        },
    });

    const deleteSkill = useCallback(
        async (id: string) => {
            await deleteAiAgentSkillMutation.mutateAsync({id});
        },
        [deleteAiAgentSkillMutation]
    );

    const handleDownloadSkill = useCallback(async (id: string, skillName: string) => {
        try {
            await downloadAiAgentSkill(id, skillName);
        } catch (error) {
            toast.error('Failed to download skill', {
                description: error instanceof Error ? error.message : 'An unexpected error occurred',
            });
        }
    }, []);

    const renameSkill = useCallback(
        (id: string, newName: string, description?: string | null) => {
            updateAiAgentSkillMutation.mutate({description: description || undefined, id, name: newName});
        },
        [updateAiAgentSkillMutation]
    );

    return {
        deleteSkill,
        filteredSkills,
        handleDownloadSkill,
        renameSkill,
        searchQuery,
        setSearchQuery,
    };
}
