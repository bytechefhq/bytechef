import {createContext, useContext} from 'react';
import type {AutomationChatConfig} from '@/types';
import type {VoiceSessionStatusType} from '@/lib/BrowserVoiceSession';

export interface AutomationChatVoiceControlsI {
    status: VoiceSessionStatusType;
    error: string | null;
    isAssistantSpeaking: boolean;
    start: () => Promise<void>;
    stop: () => void;
}

export type AutomationChatContextValueType = {
    chatMode: boolean;
    description: string;
    suggestions: AutomationChatConfig['suggestions'];
    title: string;
    voice?: AutomationChatVoiceControlsI;
    voiceEnabled: boolean;
    voiceMode: boolean;
    webhookUrl: string;
};

export const AutomationChatContext = createContext<AutomationChatContextValueType | undefined>(undefined);

export const useAutomationChatConfig = () => {
    const context = useContext(AutomationChatContext);

    if (!context) {
        throw new Error('useAutomationChatConfig must be used within AutomationChatProvider');
    }

    return context;
};
