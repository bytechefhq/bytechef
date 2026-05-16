# @bytechef/chat

ByteChef Automation Chat SDK - A React-based embeddable chat widget for workflow automation powered by assistant-ui.

## Features

- 🚀 **Easy Integration** - Simple React components that work anywhere
- 💬 **Two Modes** - Embeddable and Modal implementations
- 📡 **Real-time Streaming** - Server-Sent Events (SSE) support for live responses
- 📎 **File Attachments** - Support for image and document uploads
- 🎨 **Customizable** - Fully styled with TailwindCSS, easy to customize
- 📱 **Responsive** - Works seamlessly on desktop and mobile
- ♿ **Accessible** - Built with accessibility in mind
- 🔒 **Type-Safe** - Full TypeScript support

## Installation

```bash
npm install @bytechef/chat
```

## Quick Start

### Embeddable Chat

```tsx
import {ByteChefChat} from '@bytechef/chat';
import '@bytechef/chat/dist/style.css';

function App() {
    return (
        <ByteChefChat
            config={{
                webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
            }}
        />
    );
}
```

### Modal Chat

```tsx
import {ByteChefChatModal} from '@bytechef/chat';
import '@bytechef/chat/dist/style.css';

function App() {
    return (
        <ByteChefChatModal
            config={{
                webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
                title: 'Chat with us',
            }}
            position="bottom-right"
        />
    );
}
```

## Configuration

### AutomationChatConfig

```typescript
interface AutomationChatConfig {
    /**
     * The full text-chat webhook URL. If the URL ends with /sse, SSE streaming is used automatically;
     * otherwise the widget falls back to plain HTTP request/response.
     */
    webhookUrl: string;

    /**
     * Optional base URL of a ByteChef browser-voice webhook. When set, the widget renders a mic button.
     * Clicking the mic opens a WebSocket to <voiceWebhookUrl>/wss after minting a session token at
     * POST <voiceWebhookUrl>/voice-session-token.
     *
     * The URL points at a workflow with a browser/v1/voiceSession trigger. See the voice quickstart
     * docs for setup: docs/voice/quickstart.md
     */
    voiceWebhookUrl?: string;

    /**
     * Welcome message title shown on the first turn.
     * @default 'Hello there!'
     */
    title?: string;

    /**
     * Welcome message description shown on the first turn.
     * @default 'How can I help you today?'
     */
    description?: string;

    /**
     * Optional list of suggestion chips shown on the welcome screen.
     */
    suggestions?: Suggestion[];
}
```

### Voice support

Voice requires a modern browser (Chrome 66+, Firefox 76+, Safari 14.1+) and a secure context (HTTPS or
localhost). The widget gates the mic button automatically — on unsupported browsers no mic appears, no
silent failure at click time.

If you embed the widget inside an `<iframe>`, the iframe MUST grant microphone access via the `allow`
attribute or `getUserMedia` is silently denied by the browser:

```html
<iframe src="https://your-site.com/chat-widget" allow="microphone"></iframe>
```

The voice session is bound to the workflow's webhook — the widget hits
`POST <voiceWebhookUrl>/voice-session-token` to mint a single-use token, then opens
`WSS <voiceWebhookUrl>/wss?sessionToken=…`. The token TTL is 60 seconds; on expiry or replay the server
closes the WebSocket with a `POLICY_VIOLATION` and the widget shows the error.

If you need to detect voice support before mounting the widget (e.g. to render a different UI on
unsupported browsers), import the helper:

```tsx
import {checkVoiceSupport} from '@bytechef/chat';

const voiceReason = checkVoiceSupport();
// null = voice works
// string = human-readable reason voice does not work in this browser
```

### ByteChefChatModal Additional Props

```typescript
interface ByteChefChatModalConfig extends ByteChefChatConfig {
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
```

## Components

### ByteChefChat

Full-page embeddable chat component.

**Props:**

- `config: ByteChefChatConfig` - Chat configuration
- `className?: string` - Custom CSS class
- `header?: React.ReactNode` - Optional header component

### ByteChefChatModal

Floating modal chat component.

**Props:**

- `config: ByteChefChatModalConfig` - Chat configuration
- `trigger?: React.ReactNode` - Custom trigger button
- `className?: string` - Custom CSS class
- `position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left'` - Modal position

## Advanced Usage

### Custom Header

```tsx
<ByteChefChat
    config={config}
    header={
        <div className="p-4 border-b">
            <h1 className="text-xl font-bold">Support Chat</h1>
            <p className="text-sm text-muted-foreground">We're here to help!</p>
        </div>
    }
/>
```

### Custom Trigger Button

```tsx
<ByteChefChatModal config={config} trigger={<button className="custom-button">Need Help?</button>} />
```

### Using the Provider Directly

```tsx
import {ByteChefChatProvider, Thread} from '@bytechef/chat';

function CustomChat() {
    return (
        <ByteChefChatProvider config={config}>
            <div className="my-custom-layout">
                <MyHeader />
                <Thread />
                <MyFooter />
            </div>
        </ByteChefChatProvider>
    );
}
```

## Styling

The SDK uses TailwindCSS and CSS variables for theming. Import the stylesheet:

```tsx
import '@bytechef/chat/dist/style.css';
```

### Customizing Theme

Override CSS variables in your global CSS:

```css
:root {
    --primary: 221.2 83.2% 53.3%;
    --primary-foreground: 210 40% 98%;
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    /* ... other variables */
}
```

## Hooks

### useSSE

Hook for Server-Sent Events connections.

```tsx
import {useSSE} from '@bytechef/chat';

const {data, error, connectionState, close} = useSSE(
    {
        url: '/api/stream',
        init: {method: 'POST', body: formData},
    },
    {
        eventHandlers: {
            stream: (data) => console.log('Stream:', data),
            result: (data) => console.log('Result:', data),
            error: (data) => console.error('Error:', data),
        },
    }
);
```

### useChatStore

Zustand store for chat state management.

```tsx
import {useChatStore} from '@bytechef/chat';

const {messages, setMessage, reset} = useChatStore();
```

## API Reference

See the [TypeScript definitions](./src/types/index.ts) for complete API documentation.

## Examples

Check out the [test-app](../test-app) directory for complete working examples of both embedded and modal implementations.

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Requirements

- React 19.2+
- Modern browser with ES2020 support

## License

MIT

## Support

For issues and questions:

- [GitHub Issues](https://github.com/bytechefhq/bytechef/issues)
- [Documentation](https://docs.bytechef.com)
- Email: support@bytechef.io
