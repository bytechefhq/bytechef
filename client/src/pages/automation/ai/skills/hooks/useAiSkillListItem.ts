import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import {AiSkill} from '@/shared/middleware/graphql';
import {useCallback, useState} from 'react';
import {useNavigate} from 'react-router-dom';

const SKILL_COLORS = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500'];

function getSkillColor(skillId: string): string {
    let hash = 0;

    for (let index = 0; index < skillId.length; index++) {
        hash = skillId.charCodeAt(index) + ((hash << 5) - hash);
    }

    return SKILL_COLORS[Math.abs(hash) % SKILL_COLORS.length];
}

interface UseAiSkillListItemPropsI {
    deleteSkill: (id: string) => Promise<void>;
    onDownload: (id: string, name: string) => void;
    onUpdate: (id: string, name: string, description: string | null) => void;
    skill: AiSkill;
}

export default function useAiSkillListItem({deleteSkill, onDownload, onUpdate, skill}: UseAiSkillListItemPropsI) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {openSkillDetail} = useAiSkillsStore();

    const navigate = useNavigate();

    const handleClick = useCallback(() => {
        openSkillDetail(skill.id, skill.name);

        navigate(`/automation/ai/skills/${skill.id}`);
    }, [navigate, openSkillDetail, skill.id, skill.name]);

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

    const handleEditSave = useCallback(
        (name: string, description: string | null) => {
            onUpdate(skill.id, name, description);

            setShowEditDialog(false);
        },
        [onUpdate, skill.id]
    );

    const skillColor = getSkillColor(skill.id);

    return {
        handleClick,
        handleDeleteClick,
        handleDownloadClick,
        handleEditSave,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
        skillColor,
    };
}
