# ByteChef Automation Chat SDK

A complete React-based chat SDK for ByteChef workflow automation, featuring both embeddable and modal implementations powered by assistant-ui.

## 📁 Structure

```
sdks/automation/chat/
├── library/          # React SDK library (Vite + TypeScript)
│   ├── src/
│   │   ├── components/    # Chat components
│   │   ├── hooks/         # Custom hooks
│   │   ├── stores/        # Zustand state management
│   │   ├── types/         # TypeScript definitions
│   │   ├── utils/         # Utility functions
│   │   └── index.ts       # Main exports
│   ├── package.json
│   ├── vite.config.ts
│   └── README.md
│
└── test-app/        # Next.js test application
    ├── app/
    │   ├── embedded/      # Embedded chat demo
    │   ├── modal/         # Modal chat demo
    │   └── page.tsx       # Home page
    ├── package.json
    └── README.md
```

## 🚀 Quick Start

### 1. Build the Library

```bash
cd library
npm install
npm run build
```

### 2. Run the Test App

```bash
cd ../test-app
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) to see the demos.

## 📦 Installation

```bash
npm install @bytechef/chat
```

## 💡 Usage

### Embeddable Chat

```tsx
import {ByteChefChat} from '@bytechef/chat';
import '@bytechef/chat/dist/style.css';

<ByteChefChat
    config={{
        webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
    }}
/>;
```

### Modal Chat

```tsx
import {ByteChefChatModal} from '@bytechef/chat';
import '@bytechef/chat/dist/style.css';

<ByteChefChatModal
    config={{
        webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
        title: 'Chat with us',
    }}
    position="bottom-right"
/>;
```

## ✨ Features

- 🎯 **Two Integration Modes** - Embeddable full-page or floating modal
- 📡 **Real-time Streaming** - SSE support for live responses
- 📎 **File Attachments** - Upload images and documents
- 🎨 **Fully Customizable** - TailwindCSS-based styling
- 📱 **Responsive Design** - Works on all devices
- ♿ **Accessible** - WCAG compliant
- 🔒 **Type-Safe** - Full TypeScript support
- ⚡ **Production Ready** - Built with Vite for optimal performance

## 📚 Documentation

- [Library README](./library/README.md) - Complete SDK documentation
- [Test App README](./test-app/README.md) - Integration examples

## 🛠️ Development

### Library Development

```bash
cd library
npm run dev        # Start Storybook
npm run build      # Build for production
npm run test       # Run tests
npm run watch      # Watch mode for development
```

### Test App Development

```bash
cd test-app
npm run dev        # Start Next.js dev server
npm run build      # Build for production
```

## 🏗️ Architecture

### Technology Stack

**Library:**

- React 19.2+ with TypeScript
- Vite 6.2 for building
- @assistant-ui/react for chat UI
- Zustand for state management
- TailwindCSS for styling
- Vitest for testing

**Test App:**

- Next.js 16 with App Router
- TypeScript
- TailwindCSS

### Key Components

- **ByteChefChat** - Embeddable full-page chat
- **ByteChefChatModal** - Floating modal chat
- **ByteChefChatProvider** - Runtime provider with SSE support
- **Thread** - Message thread UI component
- **useSSE** - Server-Sent Events hook
- **useChatStore** - Zustand state management

## 🧪 Testing

Run tests in the library:

```bash
cd library
npm run test              # Run tests
npm run test:watch        # Watch mode
npm run test:coverage     # Coverage report
```

## 📝 License

MIT - See LICENSE file for details

## 🤝 Contributing

Contributions are welcome! Please read the contributing guidelines in the main ByteChef repository.

## 💬 Support

- [GitHub Issues](https://github.com/bytechefhq/bytechef/issues)
- [Documentation](https://docs.bytechef.com)
- Email: support@bytechef.io

## 🔗 Related

- [ByteChef Main Repository](https://github.com/bytechefhq/bytechef)
- [ByteChef Embedded SDK](../../frontend/embedded)
- [assistant-ui](https://github.com/assistant-ui/assistant-ui)
