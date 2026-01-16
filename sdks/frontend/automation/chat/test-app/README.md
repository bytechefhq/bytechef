# ByteChef Automation Chat - Test Application

This is a Next.js 16 test application demonstrating the integration of the ByteChef Automation Chat SDK in both embeddable and modal modes.

## Getting Started

### Prerequisites

- Node.js 20.19 or higher
- npm or yarn

### Installation

1. Install dependencies for both the library and test app:

```bash
# From the library directory
cd ../library
npm install
npm run build

# From the test-app directory
cd ../test-app
npm install
```

2. Update the configuration in the demo pages:
    - Edit `app/embedded/page.tsx`
    - Edit `app/modal/page.tsx`
    - Replace the `webhookUrl` with your actual ByteChef webhook URL

### Running the Application

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Features Demonstrated

### Embedded Chat (`/embedded`)

- Full-page chat integration
- Custom header with navigation
- Environment indicator
- Real-time message streaming

### Modal Chat (`/modal`)

- Floating chat button
- Modal overlay with backdrop
- Customizable position
- Persistent across page scrolls
- Custom trigger support

## Project Structure

```
test-app/
├── app/
│   ├── embedded/
│   │   └── page.tsx       # Embedded chat demo
│   ├── modal/
│   │   └── page.tsx       # Modal chat demo
│   ├── layout.tsx         # Root layout
│   ├── page.tsx           # Home page
│   └── globals.css        # Global styles
├── public/                # Static assets
├── package.json
├── tsconfig.json
└── next.config.js
```

## Configuration Examples

### Basic Configuration

```tsx
<ByteChefChat
    config={{
        webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
    }}
/>
```

### With Custom Header

```tsx
<ByteChefChat
    config={{
        webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
    }}
    className="custom-chat-container"
    header={<CustomHeader />}
/>
```

### Modal with Custom Trigger

```tsx
<ByteChefChatModal
    config={{
        webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
        title: 'Support Chat',
        description: 'Chat with our AI assistant',
    }}
    trigger={<button className="custom-trigger">Need Help?</button>}
    position="bottom-left"
/>
```

## Customization

### Styling

The test app imports the SDK styles and extends them with custom CSS:

```css
/* app/globals.css */
@import '@bytechef/automation-chat/dist/style.css';

/* Your custom styles */
```

### Theme Variables

Customize the chat appearance by overriding CSS variables:

```css
:root {
    --primary: 221.2 83.2% 53.3%;
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    /* ... other variables */
}
```

## Development Tips

### Live Updates

When developing the library alongside the test app:

1. In the library directory:

```bash
npm run watch
```

2. In the test-app directory:

```bash
npm run dev
```

### Building for Production

```bash
npm run build
npm run start
```

## Troubleshooting

### Module Not Found

If you see `Cannot find module '@bytechef/automation-chat'`:

1. Make sure the library is built:

```bash
cd ../library && npm run build
```

2. Reinstall dependencies:

```bash
cd ../test-app && npm install
```

### Styles Not Loading

Make sure you're importing the CSS in your layout:

```tsx
import '@bytechef/automation-chat/dist/style.css';
```

### Connection Issues

Check that:

- Your ByteChef instance is running
- The `webhookUrl` is valid and accessible
- CORS is configured properly on your ByteChef instance

## Learn More

- [ByteChef Documentation](https://docs.bytechef.com)
- [Next.js Documentation](https://nextjs.org/docs)
- [SDK README](../library/README.md)

## Support

For issues specific to this test app, please check the [main repository issues](https://github.com/bytechefhq/bytechef/issues).
