import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
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

describe('KnowledgeBaseHeader', () => {
    it('renders header component', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName="Test KB" />);

        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    it('renders knowledge base name', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName="Test KB" />);

        expect(screen.getByText('Test KB')).toBeInTheDocument();
    });

    it('renders Loading... when name is undefined', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName={undefined} />);

        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    // The sidebar lists every knowledge base and the header carries the sidebar toggle, so a back arrow would
    // only duplicate a one-click way back.
    it('renders no back button', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName="Test KB" />);

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
});
