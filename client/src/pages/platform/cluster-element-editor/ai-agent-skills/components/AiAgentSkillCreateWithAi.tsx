import {Thread} from '@/components/assistant-ui/thread';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';

const AiAgentSkillCreateWithAi = () => (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col py-6">
        <CopilotRuntimeProvider>
            <Thread />
        </CopilotRuntimeProvider>
    </div>
);

export default AiAgentSkillCreateWithAi;
