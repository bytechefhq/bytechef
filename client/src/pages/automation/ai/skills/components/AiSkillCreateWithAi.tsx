import {Thread} from '@/components/assistant-ui/thread';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useAiDefaultModelQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {type SuggestionConfig} from '@assistant-ui/react';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

type CreateWithAiOriginType = 'detail' | 'list';

interface CreateWithAiLocationStateI {
    origin?: CreateWithAiOriginType;
}

interface AiSkillsQueryDataI {
    aiSkills?: {id: string}[];
}

function extractSkillIds(data: AiSkillsQueryDataI | undefined): Set<string> {
    return new Set(data?.aiSkills?.map((skill) => skill.id) ?? []);
}

const SKILL_SUGGESTIONS: SuggestionConfig[] = [
    {
        label: 'that summarizes my unread emails daily',
        prompt: 'Create a skill that summarizes my unread Gmail and sends me a daily digest',
        title: 'Create a skill',
    },
    {
        label: 'to create a great skill?',
        prompt: 'What information do you need from me to create a great skill?',
        title: 'What should I tell you',
    },
    {
        label: 'of a well-structured SKILL.md',
        prompt: 'Show me an example of a well-structured SKILL.md',
        title: 'Show me an example',
    },
    {
        label: 'a skill from scratch — walk me through it',
        prompt: 'Help me design a skill from scratch — walk me through the questions you need answered',
        title: 'Help me design',
    },
];

const AiSkillCreateWithAi = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const queryClient = useQueryClient();

    const setContext = useCopilotStore((state) => state.setContext);
    const resetMessages = useCopilotStore((state) => state.resetMessages);
    const generateConversationId = useCopilotStore((state) => state.generateConversationId);
    const selectedLlmModel = useCopilotStore((state) => state.selectedLlmModel);
    const selectedLlmProvider = useCopilotStore((state) => state.selectedLlmProvider);
    const setSelectedLlm = useCopilotStore((state) => state.setSelectedLlm);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: defaultModelData} = useAiDefaultModelQuery({environment: String(currentEnvironmentId)});

    const origin: CreateWithAiOriginType = (location.state as CreateWithAiLocationStateI | null)?.origin ?? 'list';

    useEffect(() => {
        resetMessages();
        generateConversationId();
        setContext({mode: MODE.BUILD, parameters: {}, source: Source.SKILLS});
    }, [generateConversationId, resetMessages, setContext]);

    useEffect(() => {
        return useCopilotPostTurnRegistry.getState().register(Source.SKILLS, async () => {
            const preTurnIds = extractSkillIds(queryClient.getQueryData(['aiSkills']));

            await queryClient.invalidateQueries({queryKey: ['aiSkills']});

            const postTurnIds = extractSkillIds(queryClient.getQueryData(['aiSkills']));

            const newId = [...postTurnIds].find((id) => !preTurnIds.has(id));

            if (newId) {
                if (origin === 'detail') {
                    navigate(`/automation/ai/skills/${newId}`);
                } else {
                    navigate('/automation/ai/skills');
                }
            }
        });
    }, [navigate, origin, queryClient]);

    return (
        <div className="mx-auto flex min-h-0 w-full max-w-2xl flex-1 flex-col overflow-hidden">
            <CopilotRuntimeProvider source={Source.SKILLS} suggestions={SKILL_SUGGESTIONS}>
                <Thread
                    leadingComposerActions={
                        currentWorkspaceId != null ? (
                            <ModelPicker
                                defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
                                environment={currentEnvironmentId}
                                onChange={(provider, model) => {
                                    writeLastUsedModel(currentWorkspaceId, provider, model);
                                    setSelectedLlm(provider, model);
                                }}
                                selectedModel={selectedLlmModel ?? readLastUsedModel(currentWorkspaceId)?.model ?? null}
                                selectedProvider={
                                    selectedLlmProvider ?? readLastUsedModel(currentWorkspaceId)?.provider ?? null
                                }
                            />
                        ) : null
                    }
                    transparent
                />
            </CopilotRuntimeProvider>
        </div>
    );
};

export default AiSkillCreateWithAi;
