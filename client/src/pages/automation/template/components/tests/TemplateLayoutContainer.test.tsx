import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

import TemplateLayoutContainer from '../TemplateLayoutContainer';

describe('TemplateLayoutContainer', () => {
    it('renders the content inside the card with the logo link', () => {
        render(
            <MemoryRouter>
                <TemplateLayoutContainer>
                    <span>template body</span>
                </TemplateLayoutContainer>
            </MemoryRouter>
        );

        expect(screen.getByText('template body')).toBeInTheDocument();
        expect(screen.getByRole('link')).toHaveAttribute('href', '/');
    });

    it('omits the logo when opened from inside the app', () => {
        render(
            <MemoryRouter>
                <TemplateLayoutContainer fromInternalFlow>
                    <span>template body</span>
                </TemplateLayoutContainer>
            </MemoryRouter>
        );

        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });
});
