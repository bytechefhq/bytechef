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
     * When true, the widget enables voice support. The realtime voice adapter is installed on the runtime
     * and the widget connects to the voice session endpoint derived from webhookUrl. Set this when the
     * deployed workflow's trigger is `browser/v1/voiceSession`.
     *
     * Combine with `chatMode` to control how voice is surfaced:
     * - `voiceMode: true, chatMode: false` (default) → full-screen VoiceModeLayout takeover.
     * - `voiceMode: true, chatMode: true` → regular chat Thread with an inline voice-mode button next
     *   to the push-to-talk mic; clicking it starts a voice session inside the composer.
     * @default false
     */
    voiceMode?: boolean;

    /**
     * When true together with `voiceMode`, the widget renders the regular chat Thread (not the full-screen
     * VoiceModeLayout) and exposes voice as an inline button next to the composer's push-to-talk mic.
     * Has no effect unless `voiceMode` is also true.
     * @default false
     */
    chatMode?: boolean;

    /**
     * Optional base URL of a browser-voice webhook (e.g. https://your-bytechef-instance.com/webhooks/<id>).
     * When set, the widget renders a mic button that opens a voice session. The token-mint endpoint and the
     * WebSocket upgrade URL are derived from this base URL.
     */
    voiceWebhookUrl?: string;

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
