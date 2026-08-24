import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import AiGuardrails from '@/ee/pages/settings/automation/ai/guardrails/AiGuardrails';
import WorkspaceSystemPrompt from '@/ee/pages/settings/automation/ai/system-prompt/WorkspaceSystemPrompt';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useNavigate, useParams} from 'react-router-dom';

const SYSTEM_PROMPT_TAB = 'system-prompt';

/**
 * Guardrails and the workspace system prompt are two halves of one policy: what agents in this workspace may say, and
 * what they are standing-instructed to do. They were separate sidebar entries under an "AI" heading; folding them into
 * one page lets that heading go and leaves the AI hub connectors entry standing on its own.
 *
 * The tab lives in the URL rather than in state so each half stays linkable — the previous routes redirect onto their
 * tab rather than dumping the reader on the default one.
 */
const AiAgents = () => {
    const {tab} = useParams();

    const navigate = useNavigate();

    const currentTab = tab === SYSTEM_PROMPT_TAB ? SYSTEM_PROMPT_TAB : 'guardrails';

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description="Set the content guardrails and the standing system prompt that apply to every AI agent in this workspace."
                    position="main"
                    title="AI Agents"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="flex size-full flex-col">
                <Tabs
                    className="px-6 pt-2 pb-4"
                    onValueChange={(value) => navigate(`/automation/settings/ai/agents/${value}`)}
                    value={currentTab}
                >
                    <TabsList>
                        <TabsTrigger value="guardrails">Guardrails</TabsTrigger>

                        <TabsTrigger value={SYSTEM_PROMPT_TAB}>System Prompt</TabsTrigger>
                    </TabsList>
                </Tabs>

                {currentTab === SYSTEM_PROMPT_TAB ? <WorkspaceSystemPrompt /> : <AiGuardrails />}
            </div>
        </LayoutContainer>
    );
};

export default AiAgents;
