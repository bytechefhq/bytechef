import AiAgentSkillCreateWithAi from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillCreateWithAi';
import AiAgentSkillDetail from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillDetail';
import AiAgentSkillUploadForm from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillUploadForm';
import AiAgentSkillWriteForm from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillWriteForm';
import AiAgentSkillsEmptyState from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillsEmptyState';
import AiAgentSkillsList from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillsList';
import useAiAgentSkills from '@/pages/platform/cluster-element-editor/ai-agent-skills/hooks/useAiAgentSkills';
import {AlertTriangleIcon, Loader2Icon} from 'lucide-react';

const AiAgentSkills = () => {
    const {isError, isLoading, skills, skillsView: currentView} = useAiAgentSkills();

    if (isLoading) {
        return (
            <div className="flex flex-1 items-center justify-center">
                <Loader2Icon className="size-6 animate-spin text-gray-400" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="flex flex-1 flex-col items-center justify-center gap-2 text-sm text-gray-500">
                <AlertTriangleIcon className="size-6 text-red-400" />

                <span>Failed to load skills. Please try again.</span>
            </div>
        );
    }

    return (
        <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            {currentView === 'empty' && <AiAgentSkillsEmptyState />}

            {currentView === 'list' && <AiAgentSkillsList skills={skills} />}

            {currentView === 'detail' && <AiAgentSkillDetail />}

            {currentView === 'writeForm' && <AiAgentSkillWriteForm />}

            {currentView === 'uploadForm' && <AiAgentSkillUploadForm />}

            {currentView === 'createWithAi' && <AiAgentSkillCreateWithAi />}
        </div>
    );
};

export default AiAgentSkills;
