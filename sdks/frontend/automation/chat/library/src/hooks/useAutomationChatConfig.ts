import {createContext, useContext} from 'react';
import type {AutomationChatConfig} from '@/types';

type AutomationChatContextValue = {
    title: string;
    description: string;
    suggestions: AutomationChatConfig['suggestions'];
};

export const AutomationChatContext = createContext<AutomationChatContextValue | undefined>(undefined);

export const useAutomationChatConfig = () => {
    const context = useContext(AutomationChatContext);

    if (!context) {
        throw new Error('useAutomationChatConfig must be used within AutomationChatProvider');
    }

    return context;
};
