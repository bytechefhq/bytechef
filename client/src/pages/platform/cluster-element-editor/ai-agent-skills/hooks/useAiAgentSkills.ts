import {useAiAgentSkillsStore} from '@/pages/platform/cluster-element-editor/ai-agent-skills/stores/useAiAgentSkillsStore';
import {useAiAgentSkillsQuery} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {toast} from 'sonner';

interface UseAiAgentSkillsOptionsI {
    enabled?: boolean;
}

export default function useAiAgentSkills(options?: UseAiAgentSkillsOptionsI) {
    const enabled = options?.enabled ?? true;

    const {setSkillsPanelOpen, setSkillsView, skillsView} = useAiAgentSkillsStore();

    const {data: aiAgentSkillsData, isError, isLoading} = useAiAgentSkillsQuery(undefined, {enabled});

    const skills = useMemo(() => aiAgentSkillsData?.aiAgentSkills ?? [], [aiAgentSkillsData]);

    const previousSkillsLengthRef = useRef(skills.length);

    const handleClose = useCallback(() => {
        const isOnSubScreen =
            skillsView === 'writeForm' ||
            skillsView === 'uploadForm' ||
            skillsView === 'createWithAi' ||
            skillsView === 'detail';

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
