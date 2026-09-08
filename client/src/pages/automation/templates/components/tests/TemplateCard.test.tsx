import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import {TemplateCard} from '../TemplateCard';

vi.mock('@/components/LazyLoadSVG/LazyLoadSVG', () => ({
    default: ({src}: {src: string}) => <img alt={src} src={src} />,
}));

describe('TemplateCard', () => {
    it('renders the title, description, categories and icons', () => {
        render(
            <MemoryRouter>
                <TooltipProvider>
                    <TemplateCard
                        authorName="ByteChef"
                        categories={['ai']}
                        description="Classifies incoming mail"
                        icons={['/gmail.svg', '/openai.svg']}
                        templateId="ai-email-classifier"
                        title="AI Email Classifier"
                    />
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(screen.getByText('AI Email Classifier')).toBeInTheDocument();
        expect(screen.getByText('Classifies incoming mail')).toBeInTheDocument();
        expect(screen.getByText('ByteChef')).toBeInTheDocument();
        expect(screen.getAllByRole('img')).toHaveLength(2);
    });
});
