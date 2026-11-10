import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseHeader from '../KnowledgeBaseHeader';

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {centerTitle?: boolean; position?: string; title: React.ReactNode}) => (
        <header data-testid="header">{title}</header>
    ),
}));

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderKnowledgeBaseHeader = (knowledgeBaseName: string | undefined) =>
    render(
        <MemoryRouter>
            <KnowledgeBaseHeader knowledgeBaseName={knowledgeBaseName} />
        </MemoryRouter>
    );

describe('KnowledgeBaseHeader', () => {
    it('renders a breadcrumb link to the knowledge bases list page', () => {
        renderKnowledgeBaseHeader('Test KB');

        expect(screen.getByRole('link', {name: 'Knowledge Bases'})).toHaveAttribute(
            'href',
            '/automation/knowledge-bases'
        );
    });

    it('renders header component', () => {
        renderKnowledgeBaseHeader('Test KB');

        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    it('renders knowledge base name', () => {
        renderKnowledgeBaseHeader('Test KB');

        expect(screen.getByText('Test KB')).toBeInTheDocument();
    });

    it('renders Loading... when name is undefined', () => {
        renderKnowledgeBaseHeader(undefined);

        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    // The sidebar lists every knowledge base and the header carries the sidebar toggle, so a back arrow would
    // only duplicate a one-click way back.
    it('renders no back button', () => {
        renderKnowledgeBaseHeader('Test KB');

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
});
