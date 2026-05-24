import {Thread, type ThreadSuggestionI} from '@/components/assistant-ui/thread';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useEffect, useRef} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

type CreateWithAiOriginType = 'detail' | 'list';

interface CreateWithAiLocationStateI {
    origin?: CreateWithAiOriginType;
}

const SKILL_SUGGESTIONS: ThreadSuggestionI[] = [
    {
        action: 'Create a skill that summarizes my unread Gmail and sends me a daily digest',
        label: 'that summarizes my unread emails daily',
        title: 'Create a skill',
    },
    {
        action: 'What information do you need from me to create a great skill?',
        label: 'to create a great skill?',
        title: 'What should I tell you',
    },
    {
        action: 'Show me an example of a well-structured SKILL.md',
        label: 'of a well-structured SKILL.md',
        title: 'Show me an example',
    },
    {
        action: 'Help me design a skill from scratch — walk me through the questions you need answered',
        label: 'a skill from scratch — walk me through it',
        title: 'Help me design',
    },
];

const AiSkillCreateWithAi = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const setContext = useCopilotStore((state) => state.setContext);
    const resetMessages = useCopilotStore((state) => state.resetMessages);
    const generateConversationId = useCopilotStore((state) => state.generateConversationId);

    const origin: CreateWithAiOriginType =
        (location.state as CreateWithAiLocationStateI | null)?.origin ?? 'list';

    const {data: skillsData} = useAiSkillsQuery();

    const baselineIdsRef = useRef<Set<string> | null>(null);

    useEffect(() => {
        resetMessages();
        generateConversationId();
        setContext({mode: MODE.BUILD, parameters: {}, source: Source.SKILLS});
    }, [generateConversationId, resetMessages, setContext]);

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
                <Thread suggestions={SKILL_SUGGESTIONS} />
            </CopilotRuntimeProvider>
        </div>
    );
};

export default AiSkillCreateWithAi;
