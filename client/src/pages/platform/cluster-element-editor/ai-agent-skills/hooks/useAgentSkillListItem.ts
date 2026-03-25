import {useAiAgentSkillsStore} from '@/pages/platform/cluster-element-editor/ai-agent-skills/stores/useAiAgentSkillsStore';
import {AgentSkill} from '@/shared/middleware/graphql';
import {useCallback, useState} from 'react';

const SKILL_COLORS = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500'];

function getSkillColor(skillId: string): string {
    let hash = 0;

    for (let index = 0; index < skillId.length; index++) {
        hash = skillId.charCodeAt(index) + ((hash << 5) - hash);
    }

    return SKILL_COLORS[Math.abs(hash) % SKILL_COLORS.length];
}

interface UseAgentSkillListItemPropsI {
    deleteSkill: (id: string) => Promise<void>;
    onDownload: (id: string, name: string) => void;
    onRename: (id: string, newName: string, description?: string | null) => void;
    skill: AgentSkill;
}

export default function useAgentSkillListItem({deleteSkill, onDownload, onRename, skill}: UseAgentSkillListItemPropsI) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showRenameDialog, setShowRenameDialog] = useState(false);

    const {openSkillDetail} = useAiAgentSkillsStore();

    const handleClick = useCallback(() => {
        openSkillDetail(skill.id, skill.name);
    }, [openSkillDetail, skill.id, skill.name]);

    const handleDeleteClick = useCallback(async () => {
        try {
            await deleteSkill(skill.id);

            setShowDeleteDialog(false);
        } catch {
            setShowDeleteDialog(false);
        }
    }, [deleteSkill, skill.id]);

    const handleDownloadClick = useCallback(() => {
        onDownload(skill.id, skill.name);
    }, [onDownload, skill.id, skill.name]);

    const handleRenameClick = useCallback(
        (newName: string) => {
            onRename(skill.id, newName, skill.description);

            setShowRenameDialog(false);
        },
        [onRename, skill.description, skill.id]
    );

    const skillColor = getSkillColor(skill.id);

    return {
        handleClick,
        handleDeleteClick,
        handleDownloadClick,
        handleRenameClick,
        setShowDeleteDialog,
        setShowRenameDialog,
        showDeleteDialog,
        showRenameDialog,
        skillColor,
    };
}
