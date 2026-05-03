import downloadAiSkill from '@/shared/components/ai-skills/utils/downloadAiSkill';
import {AiSkill, useDeleteAiSkillMutation, useUpdateAiSkillMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';
import {toast} from 'sonner';

export default function useAiSkillsList(skills: AiSkill[]) {
    const [searchQuery, setSearchQuery] = useState('');

    const queryClient = useQueryClient();

    const filteredSkills = useMemo(
        () => skills.filter((skill) => skill.name.toLowerCase().includes(searchQuery.toLowerCase())),
        [searchQuery, skills]
    );

    const deleteAiSkillMutation = useDeleteAiSkillMutation({
        onError: (error: Error) => {
            toast.error('Failed to delete skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiSkills']});
        },
    });

    const updateAiSkillMutation = useUpdateAiSkillMutation({
        onError: (error: Error) => {
            toast.error('Failed to rename skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiSkills']});
        },
    });

    const deleteSkill = useCallback(
        async (id: string) => {
            await deleteAiSkillMutation.mutateAsync({id});
        },
        [deleteAiSkillMutation]
    );

    const handleDownloadSkill = useCallback(async (id: string, skillName: string) => {
        try {
            await downloadAiSkill(id, skillName);
        } catch (error) {
            toast.error('Failed to download skill', {
                description: error instanceof Error ? error.message : 'An unexpected error occurred',
            });
        }
    }, []);

    const renameSkill = useCallback(
        (id: string, newName: string, description?: string | null) => {
            updateAiSkillMutation.mutate({description: description || undefined, id, name: newName});
        },
        [updateAiSkillMutation]
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
