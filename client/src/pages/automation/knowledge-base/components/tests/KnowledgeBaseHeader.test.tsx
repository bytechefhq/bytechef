import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseHeader from '../KnowledgeBaseHeader';

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {centerTitle?: boolean; position?: string; title: React.ReactNode}) => (
        <header data-testid="header">{title}</header>
    ),
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({icon, onClick}: {icon?: React.ReactNode; onClick?: () => void; size?: string; variant?: string}) => (
        <button data-testid="back-button" onClick={onClick}>
            {icon}
        </button>
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
    const mockOnBackClick = vi.fn();

    it('renders header component', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName="Test KB" onBackClick={mockOnBackClick} />);

        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    it('renders knowledge base name', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName="Test KB" onBackClick={mockOnBackClick} />);

        expect(screen.getByText('Test KB')).toBeInTheDocument();
    });

    it('renders Loading... when name is undefined', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName={undefined} onBackClick={mockOnBackClick} />);

        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('renders back button', () => {
        render(<KnowledgeBaseHeader knowledgeBaseName="Test KB" onBackClick={mockOnBackClick} />);

        expect(screen.getByTestId('back-button')).toBeInTheDocument();
    });

    it('calls onBackClick when back button is clicked', async () => {
        render(<KnowledgeBaseHeader knowledgeBaseName="Test KB" onBackClick={mockOnBackClick} />);

        const backButton = screen.getByTestId('back-button');
        await userEvent.click(backButton);

        expect(mockOnBackClick).toHaveBeenCalled();
    });
});
