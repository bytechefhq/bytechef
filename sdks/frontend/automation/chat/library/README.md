# @bytechef/automation-chat

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
npm install @bytechef/automation-chat
```

## Quick Start

### Embeddable Chat

```tsx
import {ByteChefChat} from '@bytechef/automation-chat';
import '@bytechef/automation-chat/dist/style.css';

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
import {ByteChefChatModal} from '@bytechef/automation-chat';
import '@bytechef/automation-chat/dist/style.css';

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

### ByteChefChatConfig

```typescript
interface ByteChefChatConfig {
    /**
     * The full webhook URL to connect to
     */
    webhookUrl: string;
}
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
import {ByteChefChatProvider, Thread} from '@bytechef/automation-chat';

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
import '@bytechef/automation-chat/dist/style.css';
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
import {useSSE} from '@bytechef/automation-chat';

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
import {useChatStore} from '@bytechef/automation-chat';

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
