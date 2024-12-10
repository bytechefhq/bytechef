import CopilotChatModelAdapter from '@/pages/platform/copilot/CopilotChatModelAdapter';
import {AssistantRuntimeProvider, useLocalRuntime} from '@assistant-ui/react';

import type {ReactNode} from 'react';

export function CopilotRuntimeProvider({
    children,
}: Readonly<{
    children: ReactNode;
}>) {
    const runtime = useLocalRuntime(CopilotChatModelAdapter);

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
