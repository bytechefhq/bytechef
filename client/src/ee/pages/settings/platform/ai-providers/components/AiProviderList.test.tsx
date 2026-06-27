import {AiProvider} from '@/ee/shared/middleware/platform/configuration';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import AiProviderList from './AiProviderList';

// ---------------------------------------------------------------------------
// Hoisted mocks
// ---------------------------------------------------------------------------

const {enableAiProviderMutateMock} = vi.hoisted(() => ({
    enableAiProviderMutateMock: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/platform/aiProvider.mutations', () => ({
    useEnableAiProviderMutation: () => ({mutate: enableAiProviderMutateMock}),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="provider icon" src={src} />,
}));

// ---------------------------------------------------------------------------
// Render helper
// ---------------------------------------------------------------------------

const renderWithProviders = (ui: React.ReactElement) => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    const wrapper = ({children}: {children: ReactNode}) => <QueryClientWrapper>{children}</QueryClientWrapper>;

    return render(ui, {wrapper});
};

// ---------------------------------------------------------------------------
// Test data
// ---------------------------------------------------------------------------

const providers: AiProvider[] = [
    {
        apiKey: 'sk-openai',
        enabled: true,
        icon: '/icons/openai.svg',
        id: 1,
        name: 'Open AI',
        supportsEmbeddings: true,
    },
    {
        apiKey: 'sk-anthropic',
        enabled: false,
        icon: '/icons/anthropic.svg',
        id: 2,
        name: 'Anthropic',
        supportsEmbeddings: false,
    },
];

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('AiProviderList', () => {
    it('renders the Embeddings badge only for embedding-capable providers', () => {
        renderWithProviders(<AiProviderList aiProviders={providers} environment={1} />);

        expect(screen.getAllByText('Embeddings')).toHaveLength(1);
    });

    it('renders provider names', () => {
        renderWithProviders(<AiProviderList aiProviders={providers} environment={1} />);

        expect(screen.getByText('Open AI')).toBeInTheDocument();
        expect(screen.getByText('Anthropic')).toBeInTheDocument();
    });

    it('does not render the Embeddings badge when no provider supports embeddings', () => {
        const noEmbeddingProviders: AiProvider[] = [
            {
                apiKey: 'sk-anthropic',
                enabled: false,
                icon: '/icons/anthropic.svg',
                id: 2,
                name: 'Anthropic',
                supportsEmbeddings: false,
            },
        ];

        renderWithProviders(<AiProviderList aiProviders={noEmbeddingProviders} environment={1} />);

        expect(screen.queryByText('Embeddings')).not.toBeInTheDocument();
    });
});
