import {Thread} from '@/components/assistant-ui/thread';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';

const AiSkillCreateWithAi = () => (
    <div className="mx-auto flex min-h-0 w-full max-w-2xl flex-1 flex-col overflow-hidden">
        <CopilotRuntimeProvider source={Source.SKILLS}>
            <Thread />
        </CopilotRuntimeProvider>
    </div>
);

export default AiSkillCreateWithAi;
