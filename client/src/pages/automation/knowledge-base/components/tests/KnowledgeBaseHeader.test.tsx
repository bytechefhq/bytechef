import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseHeader from '../KnowledgeBaseHeader';

// The sidebar toggle in the title renders a Tooltip, which throws outside a provider.
const renderHeader = (knowledgeBaseName: string | undefined) =>
    render(
        <TooltipProvider>
            <KnowledgeBaseHeader knowledgeBaseName={knowledgeBaseName} />
        </TooltipProvider>
    );

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
    it('renders header component', () => {
        renderHeader('Test KB');

        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    it('renders knowledge base name', () => {
        renderHeader('Test KB');

        expect(screen.getByText('Test KB')).toBeInTheDocument();
    });

    it('renders Loading... when name is undefined', () => {
        renderHeader(undefined);

        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });
});
