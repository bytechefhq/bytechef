import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';

export function getUserTurnCount(): number {
    return useCopilotStore.getState().messages.filter((message) => message.role === 'user').length;
}
