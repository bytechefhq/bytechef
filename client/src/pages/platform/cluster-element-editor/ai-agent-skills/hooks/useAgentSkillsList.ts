import downloadAgentSkill from '@/pages/platform/cluster-element-editor/ai-agent-skills/utils/downloadAgentSkill';
import {AgentSkill, useDeleteAgentSkillMutation, useUpdateAgentSkillMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';
import {toast} from 'sonner';

export default function useAgentSkillsList(skills: AgentSkill[]) {
    const [searchQuery, setSearchQuery] = useState('');

    const queryClient = useQueryClient();

    const filteredSkills = useMemo(
        () => skills.filter((skill) => skill.name.toLowerCase().includes(searchQuery.toLowerCase())),
        [searchQuery, skills]
    );

    const deleteAgentSkillMutation = useDeleteAgentSkillMutation({
        onError: (error: Error) => {
            toast.error('Failed to delete skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['agentSkills']});
        },
    });

    const updateAgentSkillMutation = useUpdateAgentSkillMutation({
        onError: (error: Error) => {
            toast.error('Failed to rename skill', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['agentSkills']});
        },
    });

    const deleteSkill = useCallback(
        async (id: string) => {
            await deleteAgentSkillMutation.mutateAsync({id});
        },
        [deleteAgentSkillMutation]
    );

    const handleDownloadSkill = useCallback(async (id: string, skillName: string) => {
        try {
            await downloadAgentSkill(id, skillName);
        } catch (error) {
            toast.error('Failed to download skill', {
                description: error instanceof Error ? error.message : 'An unexpected error occurred',
            });
        }
    }, []);

    const renameSkill = useCallback(
        (id: string, newName: string, description?: string | null) => {
            updateAgentSkillMutation.mutate({description: description || undefined, id, name: newName});
        },
        [updateAgentSkillMutation]
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
