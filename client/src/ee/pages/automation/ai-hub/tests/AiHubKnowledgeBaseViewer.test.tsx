import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AiHubKnowledgeBaseViewer from '../AiHubKnowledgeBaseViewer';

vi.mock('@/pages/automation/knowledge-base/EmbeddableKnowledgeBase', () => ({
    default: ({knowledgeBaseId}: {knowledgeBaseId: string}) => (
        <div data-knowledgebase-id={knowledgeBaseId} data-testid="embeddable-knowledge-base" />
    ),
}));

const wrap = (ui: React.ReactNode) => render(<MemoryRouter>{ui}</MemoryRouter>);

describe('AiHubKnowledgeBaseViewer', () => {
    it('renders the KB name and Open in full view link with the correct href', () => {
        wrap(<AiHubKnowledgeBaseViewer knowledgeBaseId="kb-42" name="My Knowledge Base" />);

        expect(screen.getByText('My Knowledge Base')).toBeInTheDocument();

        const link = screen.getByRole('link', {name: /open in full view/i});

        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', '/automation/knowledge-bases/kb-42');
        expect(link).toHaveAttribute('target', '_blank');
    });

    it('renders EmbeddableKnowledgeBase with the correct knowledgeBaseId', () => {
        wrap(<AiHubKnowledgeBaseViewer knowledgeBaseId="kb-42" name="My Knowledge Base" />);

        const embed = screen.getByTestId('embeddable-knowledge-base');

        expect(embed).toBeInTheDocument();
        expect(embed).toHaveAttribute('data-knowledgebase-id', 'kb-42');
    });
});
