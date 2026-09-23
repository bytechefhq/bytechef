import {createContext} from 'react';

interface HubBuilderContextValueI {
    connectionDialogAllowed: boolean;
    includeComponents?: string[];
    onBack?: () => void;
}

export const HubBuilderContext = createContext<HubBuilderContextValueI | null>(null);
