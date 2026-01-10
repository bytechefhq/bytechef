'use client';

import {useState} from 'react';
import dynamic from 'next/dynamic';
import Link from 'next/link';

const AutomationChat = dynamic(
    () => import('@bytechef/automation-chat').then((mod) => ({default: mod.AutomationChat})),
    {
        ssr: false,
        loading: () => <div className="flex size-full items-center justify-center">Loading chat...</div>,
    }
);

export default function EmbeddedPage() {
    const [webhookUrl, setWebhookUrl] = useState('http://localhost:8080/webhooks/your-webhook-id-here/sse');

    return (
        <div className="flex h-screen flex-col">
            {/* Header */}
            <header className="border-b border-border bg-background px-4 py-3">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <Link href="/" className="text-sm text-muted-foreground hover:text-foreground">
                            ← Back
                        </Link>
                        <h1 className="text-lg font-semibold">Embedded Chat Demo</h1>
                    </div>
                    <div className="text-sm text-muted-foreground">
                        Environment: <span className="font-semibold">Development</span>
                    </div>
                </div>
            </header>

            {/* Configuration */}
            <div className="border-b border-border bg-muted/50 px-4 py-3">
                <div className="flex items-center gap-3">
                    <label htmlFor="webhookUrl" className="text-sm font-medium">
                        Webhook URL:
                    </label>
                    <input
                        id="webhookUrl"
                        type="text"
                        value={webhookUrl}
                        onChange={(e) => setWebhookUrl(e.target.value)}
                        className="flex-1 rounded-md border border-input bg-background px-3 py-1.5 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                        placeholder="http://localhost:8080/webhooks/your-webhook-id-here(/sse)"
                    />
                    <span className="text-xs text-muted-foreground">
                        {webhookUrl.endsWith('/sse') ? '🔄 SSE Streaming' : '📨 HTTP Mode'}
                    </span>
                </div>
            </div>

            {/* Chat Container */}
            <div className="flex-1 overflow-hidden">
                <AutomationChat
                    config={{
                        title: 'Hello there!',
                        description: 'How can I help you today?',
                        webhookUrl: webhookUrl,
                        suggestions: [
                            {
                                title: "What's the weather",
                                label: 'in San Francisco?',
                                prompt: "What's the weather in San Francisco?",
                            },
                            {
                                title: 'Explain React hooks',
                                label: 'like useState and useEffect',
                                prompt: 'Explain React hooks like useState and useEffect',
                            },
                        ],
                    }}
                />
            </div>
        </div>
    );
}
