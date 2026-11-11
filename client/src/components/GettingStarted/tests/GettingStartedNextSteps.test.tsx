import GettingStartedNextSteps from '@/components/GettingStarted/GettingStartedNextSteps';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

describe('GettingStartedNextSteps', () => {
    it('renders external steps as new-tab anchors and internal steps as router links', () => {
        render(
            <MemoryRouter>
                <GettingStartedNextSteps
                    steps={[
                        {description: 'Docs', external: true, href: 'https://example.com', title: 'External'},
                        {description: 'Page', external: false, href: '/internal', title: 'Internal'},
                    ]}
                />
            </MemoryRouter>
        );

        expect(screen.getByRole('link', {name: 'External'})).toHaveAttribute('target', '_blank');
        expect(screen.getByRole('link', {name: 'Internal'})).toHaveAttribute('href', '/internal');
    });
});
