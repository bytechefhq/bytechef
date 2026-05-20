import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import {useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {toast} from 'sonner';

interface UseAiSkillsOptionsI {
    enabled?: boolean;
}

export default function useAiSkills(options?: UseAiSkillsOptionsI) {
    const enabled = options?.enabled ?? true;

    const {setSkillsPanelOpen, setSkillsView, skillsView} = useAiSkillsStore();

    const {data: aiSkillsData, isError, isLoading} = useAiSkillsQuery(undefined, {enabled});

    const skills = useMemo(() => aiSkillsData?.aiSkills ?? [], [aiSkillsData]);

    const previousSkillsLengthRef = useRef(skills.length);

    const handleClose = useCallback(() => {
        const isOnSubScreen = skillsView === 'writeForm' || skillsView === 'uploadForm' || skillsView === 'detail';

        if (isOnSubScreen) {
            setSkillsView('list');
        } else {
            setSkillsPanelOpen(false);
        }
    }, [setSkillsPanelOpen, setSkillsView, skillsView]);

    // Sync the store view when the skill list becomes empty or non-empty
    useEffect(() => {
        const wasEmpty = previousSkillsLengthRef.current === 0;
        const isEmpty = skills.length === 0;

        previousSkillsLengthRef.current = skills.length;

        if (wasEmpty && !isEmpty && skillsView === 'empty') {
            setSkillsView('list');
        } else if (!wasEmpty && isEmpty && skillsView === 'list') {
            setSkillsView('empty');
        } else if (skillsView === 'empty' && skills.length > 0) {
            setSkillsView('list');
        } else if (skillsView === 'list' && skills.length === 0) {
            setSkillsView('empty');
        }
    }, [skills.length, setSkillsView, skillsView]);

    useEffect(() => {
        if (isError) {
            toast.error('Failed to load skills', {id: 'skills-load-error'});
        }
    }, [isError]);

    return {
        handleClose,
        isError,
        isLoading,
        skills,
        skillsView,
    };
}
