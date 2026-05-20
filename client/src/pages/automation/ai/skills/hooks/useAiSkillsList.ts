import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import downloadAiSkill from '@/pages/automation/ai/skills/utils/downloadAiSkill';
import {AiSkill, useDeleteAiSkillMutation, useUpdateAiSkillMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {toast} from 'sonner';

export default function useAiSkillsList(skills: AiSkill[]) {
    const searchQuery = useAiSkillsStore((state) => state.searchQuery);

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

    const editDescriptionSkill = useCallback(
        (id: string, description: string | null) => {
            const skill = skills.find((currentSkill) => currentSkill.id === id);

            if (!skill) {
                return;
            }

            updateAiSkillMutation.mutate({description: description || undefined, id, name: skill.name});
        },
        [skills, updateAiSkillMutation]
    );

    const renameSkill = useCallback(
        (id: string, newName: string, description?: string | null) => {
            updateAiSkillMutation.mutate({description: description || undefined, id, name: newName});
        },
        [updateAiSkillMutation]
    );

    return {
        deleteSkill,
        editDescriptionSkill,
        filteredSkills,
        handleDownloadSkill,
        renameSkill,
    };
}
