import {fireEvent, render, screen, stubMutation} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiGatewayProviderDialog from '../AiGatewayProviderDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    AiGatewayProviderType: {
        Anthropic: 'ANTHROPIC',
        AzureOpenai: 'AZURE_OPENAI',
        Cohere: 'COHERE',
        Deepseek: 'DEEPSEEK',
        GoogleGemini: 'GOOGLE_GEMINI',
        Groq: 'GROQ',
        Mistral: 'MISTRAL',
        Openai: 'OPENAI',
    },
    useCreateWorkspaceAiGatewayProviderMutation: vi.fn(),
    useTestWorkspaceAiGatewayProviderConnectionMutation: vi.fn(),
    useUpdateWorkspaceAiGatewayProviderMutation: vi.fn(),
}));

const {
    useCreateWorkspaceAiGatewayProviderMutation,
    useTestWorkspaceAiGatewayProviderConnectionMutation,
    useUpdateWorkspaceAiGatewayProviderMutation,
} = await import('@/shared/middleware/graphql');

const createProviderMutation = stubMutation(vi.mocked(useCreateWorkspaceAiGatewayProviderMutation));
const testProviderConnectionMutation = stubMutation(vi.mocked(useTestWorkspaceAiGatewayProviderConnectionMutation));
const updateProviderMutation = stubMutation(vi.mocked(useUpdateWorkspaceAiGatewayProviderMutation));

beforeEach(() => {
    createProviderMutation.mutate.mockClear();
    testProviderConnectionMutation.mutate.mockClear();
    updateProviderMutation.mutate.mockClear();
});

const renderDialog = (onClose = vi.fn()) => {
    render(<AiGatewayProviderDialog onClose={onClose} workspaceId="1" />);

    return onClose;
};

describe('AiGatewayProviderDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Add Provider'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Type')).toBeInTheDocument();
        expect(screen.getByLabelText('API Key')).toBeInTheDocument();
        expect(screen.getByLabelText('Base URL (optional)')).toBeInTheDocument();
    });

    it('renders the type control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Type')).toHaveAttribute('data-slot', 'select-trigger');
    });
});
