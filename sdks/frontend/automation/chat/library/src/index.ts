// Main components
export {AutomationChat} from './components/AutomationChat';
export {AutomationChatModal} from './components/AutomationChatModal';
export {AutomationChatProvider} from './components/AutomationChatProvider';

// UI Components
export {Thread} from './components/assistant-ui/thread';
export {Button} from './components/ui/button';
export {TooltipIconButton} from './components/assistant-ui/tooltip-icon-button';

// Hooks
export {useSSE} from './hooks/useSSE';
export type {UseSSEOptionsType, SSERequestType, UseSSEResultType} from './hooks/useSSE';
export {useAutomationChatConfig} from './hooks/useAutomationChatConfig';

// Store
export {useChatStore} from './stores/useChatStore';

// Types
export type {AutomationChatConfig, AutomationChatModalConfig, ChatMessage, Suggestion} from './types';

// Utils
export {cn} from './utils/cn';
export {extractStreamChunk} from './utils/stream-utils';
