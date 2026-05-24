import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import getSkillColor from '@/pages/automation/ai/skills/utils/getSkillColor';
import {AiSkill} from '@/shared/middleware/graphql';
import {useCallback, useState} from 'react';
import {useNavigate} from 'react-router-dom';

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
