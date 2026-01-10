'use client';

import {useState} from 'react';
import dynamic from 'next/dynamic';
import Link from 'next/link';

const AutomationChatModal = dynamic(
    () => import('@bytechef/automation-chat').then((mod) => ({default: mod.AutomationChatModal})),
    {
        ssr: false,
    }
);

export default function ModalPage() {
    const [webhookUrl, setWebhookUrl] = useState('http://localhost:8080/webhooks/your-webhook-id-here/sse');

    return (
        <div className="min-h-screen bg-background">
            {/* Header */}
            <header className="border-b border-border bg-background px-4 py-3">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <Link href="/" className="text-sm text-muted-foreground hover:text-foreground">
                            ← Back
                        </Link>
                        <h1 className="text-lg font-semibold">Modal Chat Demo</h1>
                    </div>
                </div>
            </header>

            {/* Content */}
            <main className="mx-auto max-w-4xl p-8">
                <div className="space-y-8">
                    <div>
                        <h2 className="text-3xl font-bold tracking-tight">Modal Chat Implementation</h2>
                        <p className="mt-2 text-muted-foreground">
                            The chat modal appears as a floating button in the bottom-right corner.
                        </p>
                    </div>

                    <div className="rounded-lg border border-border bg-muted/50 p-6">
                        <h3 className="mb-4 text-lg font-semibold">Configuration</h3>
                        <div className="flex items-center gap-3">
                            <label htmlFor="webhookUrl" className="text-sm font-medium">
                                Webhook URL:
                            </label>
                            <input
                                id="webhookUrl"
                                type="text"
                                value={webhookUrl}
                                onChange={(e) => setWebhookUrl(e.target.value)}
                                className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                                placeholder="http://localhost:8080/webhooks/your-webhook-id-here(/sse)"
                            />
                            <span className="text-xs text-muted-foreground">
                                {webhookUrl.endsWith('/sse') ? '🔄 SSE Streaming' : '📨 HTTP Mode'}
                            </span>
                        </div>
                    </div>

                    <div className="rounded-lg border border-border bg-card p-6">
                        <h3 className="mb-4 text-xl font-semibold">Features</h3>
                        <ul className="space-y-2 text-sm">
                            <li className="flex items-start gap-2">
                                <span className="mt-1 text-primary">✓</span>
                                <span>Floating chat button that stays visible while scrolling</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="mt-1 text-primary">✓</span>
                                <span>Modal overlay with backdrop</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="mt-1 text-primary">✓</span>
                                <span>Responsive design that works on mobile and desktop</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="mt-1 text-primary">✓</span>
                                <span>Customizable position (bottom-right, bottom-left, top-right, top-left)</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="mt-1 text-primary">✓</span>
                                <span>Custom trigger button support</span>
                            </li>
                        </ul>
                    </div>

                    <div className="rounded-lg border border-border bg-muted/50 p-6">
                        <h3 className="mb-4 text-lg font-semibold">Usage Example</h3>
                        <pre className="overflow-x-auto rounded bg-black p-4 text-sm text-white">
                            {`<AutomationChatModal
  config={{
    webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
    title: 'Chat with us',
  }}
  position="bottom-right"
/>`}
                        </pre>
                    </div>

                    <div className="space-y-4">
                        <p className="text-muted-foreground">
                            Scroll down to see how the chat button remains accessible throughout the page.
                        </p>
                        {Array.from({length: 5}).map((_, i) => (
                            <div key={i} className="rounded-lg border border-border bg-card p-6">
                                <h4 className="mb-2 font-semibold">Sample Content Section {i + 1}</h4>
                                <p className="text-sm text-muted-foreground">
                                    This is placeholder content to demonstrate how the floating chat button remains
                                    accessible while scrolling through your page. The modal can be opened from anywhere
                                    on the page.
                                </p>
                            </div>
                        ))}
                    </div>
                </div>
            </main>

            {/* Chat Modal */}
            <AutomationChatModal
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
                position="bottom-right"
            />
        </div>
    );
}
