export type Suggestion = {
    title: string;
    label: string;
    prompt: string;
};

export interface AutomationChatConfig {
    /**
     * The full webhook URL to connect to
     * If the URL ends with '/sse', SSE streaming will be used automatically
     */
    webhookUrl: string;

    /**
     * Welcome message title
     * @default 'Hello there!'
     */
    title?: string;

    /**
     * Welcome message description
     * @default 'How can I help you today?'
     */
    description?: string;

    /**
     * Optional suggestions to display in the welcome screen
     */
    suggestions?: Suggestion[];
}

export interface AutomationChatModalConfig extends AutomationChatConfig {
    /**
     * Modal title
     * @default 'Chat'
     */
    title?: string;

    /**
     * Modal description for accessibility
     */
    description?: string;
}

export type ChatMessage = {
    role: 'user' | 'assistant';
    content: string;
    attachments?: File[];
};
