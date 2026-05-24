import {Thread} from '@/components/assistant-ui/thread';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useEffect, useRef} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

type CreateWithAiOriginType = 'detail' | 'list';

interface CreateWithAiLocationStateI {
    origin?: CreateWithAiOriginType;
}

const AiSkillCreateWithAi = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const origin: CreateWithAiOriginType =
        (location.state as CreateWithAiLocationStateI | null)?.origin ?? 'list';

    const {data: skillsData} = useAiSkillsQuery();

    const baselineIdsRef = useRef<Set<string> | null>(null);

    useEffect(() => {
        if (!skillsData?.aiSkills) {
            return;
        }

        const currentIds = new Set(skillsData.aiSkills.map((skill) => skill.id));

        if (baselineIdsRef.current === null) {
            baselineIdsRef.current = currentIds;

            return;
        }

        const newId = [...currentIds].find((id) => !baselineIdsRef.current?.has(id));

        if (newId) {
            if (origin === 'detail') {
                navigate(`/automation/ai/skills/${newId}`);
            } else {
                navigate('/automation/ai/skills');
            }
        }
    }, [navigate, origin, skillsData]);

    return (
        <div className="mx-auto flex min-h-0 w-full max-w-2xl flex-1 flex-col overflow-hidden">
            <CopilotRuntimeProvider source={Source.SKILLS}>
                <Thread />
            </CopilotRuntimeProvider>
        </div>
    );
};

export default AiSkillCreateWithAi;
