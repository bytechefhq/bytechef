import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import {AiSkill} from '@/shared/middleware/graphql';
import {AI_SKILLS_SETTINGS_PATH} from '@/shared/navigation/settingsPaths';
import {useCallback, useState} from 'react';
import {useNavigate} from 'react-router-dom';

interface UseAiSkillListItemPropsI {
    deleteSkill: (id: string) => Promise<void>;
    onDownload: (id: string, name: string) => void;
    onUpdate: (id: string, name: string, description: string | null) => void;
    skill: AiSkill;
}

export default function useAiSkillListItem({deleteSkill, onDownload, onUpdate, skill}: UseAiSkillListItemPropsI) {
    const [isDeletePending, setIsDeletePending] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {openSkillDetail} = useAiSkillsStore();

    const navigate = useNavigate();

    const handleClick = useCallback(() => {
        openSkillDetail(skill.id, skill.name);

        void navigate(`${AI_SKILLS_SETTINGS_PATH}/${skill.id}`);
    }, [navigate, openSkillDetail, skill.id, skill.name]);

    const handleDeleteClick = useCallback(async () => {
        setIsDeletePending(true);

        try {
            await deleteSkill(skill.id);

            setShowDeleteDialog(false);
        } catch {
            setShowDeleteDialog(false);
        } finally {
            setIsDeletePending(false);
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

    return {
        handleClick,
        handleDeleteClick,
        handleDownloadClick,
        handleEditSave,
        isDeletePending,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
    };
}
