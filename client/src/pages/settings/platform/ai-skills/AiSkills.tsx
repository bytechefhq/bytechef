import AiSkillCreateWithAi from '@/shared/components/ai-skills/components/AiSkillCreateWithAi';
import AiSkillDetail from '@/shared/components/ai-skills/components/AiSkillDetail';
import AiSkillUploadForm from '@/shared/components/ai-skills/components/AiSkillUploadForm';
import AiSkillWriteForm from '@/shared/components/ai-skills/components/AiSkillWriteForm';
import AiSkillsEmptyState from '@/shared/components/ai-skills/components/AiSkillsEmptyState';
import AiSkillsList from '@/shared/components/ai-skills/components/AiSkillsList';
import useAiSkills from '@/shared/components/ai-skills/hooks/useAiSkills';
import {AlertTriangleIcon, Loader2Icon} from 'lucide-react';

const AiSkills = () => {
    const {isError, isLoading, skills, skillsView: currentView} = useAiSkills();

    if (isLoading) {
        return (
            <div className="flex flex-1 items-center justify-center">
                <Loader2Icon className="size-6 animate-spin text-gray-400" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="flex flex-1 flex-col items-center justify-center gap-2 text-sm text-content-neutral-secondary">
                <AlertTriangleIcon className="size-6 text-red-400" />

                <span>Failed to load skills. Please try again.</span>
            </div>
        );
    }

    return (
        <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            {currentView === 'empty' && <AiSkillsEmptyState />}

            {currentView === 'list' && <AiSkillsList skills={skills} />}

            {currentView === 'detail' && <AiSkillDetail />}

            {currentView === 'writeForm' && <AiSkillWriteForm />}

            {currentView === 'uploadForm' && <AiSkillUploadForm />}

            {currentView === 'createWithAi' && <AiSkillCreateWithAi />}
        </div>
    );
};

export default AiSkills;
