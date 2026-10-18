import AiHubMessageContent from '@/ee/pages/automation/ai-hub/messages/AiHubMessageContent';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

interface MockComponentRegistryI {
    Source: (props: {title?: string; url?: string}) => ReactNode;
    Text: () => ReactNode;
    data: {by_name: Record<string, (props: {data: unknown}) => ReactNode>};
    tools: {Fallback: (props: {args?: unknown; result?: unknown; toolCallId?: string; toolName: string}) => ReactNode};
}

let lastComponentsArg: MockComponentRegistryI | null = null;

vi.mock('@assistant-ui/react', () => ({
    MessagePrimitive: {
        Parts: (props: {components: MockComponentRegistryI}) => {
            lastComponentsArg = props.components;

            return (
                <div data-testid="parts">
                    {props.components.Text()}

                    {props.components.tools.Fallback({
                        args: {label: 'demo'},
                        result: {ok: true},
                        toolCallId: 'call-demo',
                        toolName: 'doThing',
                    })}

                    {props.components.data.by_name['create-connection']({
                        data: {
                            componentLabel: 'Slack',
                            componentName: 'slack',
                            kind: 'create-connection',
                        },
                    })}

                    {props.components.Source({title: 'docs', url: 'https://example.com/docs'})}
                </div>
            );
        },
    },
}));

vi.mock('@/components/assistant-ui/markdown-text', () => ({
    MarkdownText: () => <div data-testid="markdown-text" />,
}));

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer', () => ({
    AiHubToolCallFallback: (props: {toolName: string; toolCallId?: string}) => (
        <div data-testid="tool-fallback">{`tool:${props.toolName}:${props.toolCallId}`}</div>
    ),
    default: () => null,
}));

vi.mock('@/shared/components/ai-chat/messages/CreateConnectionMessage', () => ({
    default: (props: {data: {componentName: string; componentLabel: string}}) => (
        <div data-testid="create-connection">{`connect:${props.data.componentName}:${props.data.componentLabel}`}</div>
    ),
}));

describe('AiHubMessageContent', () => {
    it('wires Text -> MarkdownText for assistant message text rendering', () => {
        render(<AiHubMessageContent />);

        expect(screen.getByTestId('markdown-text')).toBeInTheDocument();
    });

    it('wires the tool-call fallback for tool-call parts', () => {
        render(<AiHubMessageContent />);

        expect(screen.getByTestId('tool-fallback')).toHaveTextContent('tool:doThing:call-demo');
    });

    it('wires the create-connection data renderer through data.by_name', () => {
        render(<AiHubMessageContent />);

        expect(screen.getByTestId('create-connection')).toHaveTextContent('connect:slack:Slack');
    });

    it('renders sources as a compact citation chip with link target', () => {
        render(<AiHubMessageContent />);

        const link = screen.getByRole('link', {name: /docs/});

        expect(link).toHaveAttribute('href', 'https://example.com/docs');
        expect(link).toHaveAttribute('target', '_blank');
    });

    it('passes a components object that exposes Text, tools.Fallback, data.by_name, and Source', () => {
        render(<AiHubMessageContent />);

        expect(lastComponentsArg).not.toBeNull();
        expect(typeof lastComponentsArg!.Text).toBe('function');
        expect(typeof lastComponentsArg!.tools.Fallback).toBe('function');
        expect(typeof lastComponentsArg!.data.by_name['create-connection']).toBe('function');
        expect(typeof lastComponentsArg!.Source).toBe('function');
    });
});
