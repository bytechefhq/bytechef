import AiSkillDetail from '@/pages/automation/ai/skills/components/AiSkillDetail';
import AiSkillsEmptyState from '@/pages/automation/ai/skills/components/AiSkillsEmptyState';
import AiSkillsList from '@/pages/automation/ai/skills/components/AiSkillsList';
import useAiSkills from '@/pages/automation/ai/skills/hooks/useAiSkills';
import {AlertTriangleIcon, Loader2Icon} from 'lucide-react';

const AiSkillsPanel = () => {
    const {isError, isLoading, skills, skillsView: currentView} = useAiSkills();

    if (isLoading) {
        return (
            <div className="flex flex-1 items-center justify-center">
                <Loader2Icon className="size-6 animate-spin text-content-neutral-tertiary" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="flex flex-1 flex-col items-center justify-center gap-2 text-sm text-content-neutral-secondary">
                <AlertTriangleIcon className="size-6 text-content-destructive" />

                <span>Failed to load skills. Please try again.</span>
            </div>
        );
    }

    return (
        <div className="flex min-w-0 flex-1 flex-col overflow-y-auto">
            {currentView === 'empty' && <AiSkillsEmptyState />}

            {currentView === 'list' && <AiSkillsList skills={skills} />}

            {currentView === 'detail' && <AiSkillDetail />}
        </div>
    );
};

export default AiSkillsPanel;
