import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import {mockScrollIntoView, render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    edition: 'EE',
    mockSetCurrentEnvironmentId: vi.fn(),
}));

vi.mock('zustand/react/shallow', () => ({
    useShallow: (selector: (state: Record<string, unknown>) => unknown) => selector,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({application: {edition: hoisted.edition}}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            currentEnvironmentId: 0,
            setCurrentEnvironmentId: hoisted.mockSetCurrentEnvironmentId,
        }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({
        data: {
            environments: [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
                {id: '2', name: 'Production'},
            ],
        },
    }),
}));

const renderEnvironmentSelect = (variant?: 'compact' | 'default' | 'icon') =>
    render(
        <MemoryRouter>
            <EnvironmentSelect variant={variant} />
        </MemoryRouter>
    );

describe('EnvironmentSelect', () => {
    beforeEach(() => {
        mockScrollIntoView();
        hoisted.edition = 'EE';
    });

    it('should render badge with current environment label when EE edition', () => {
        renderEnvironmentSelect();

        expect(screen.getByText('DEVELOPMENT')).toBeInTheDocument();
    });

    it('should not render when edition is CE', () => {
        hoisted.edition = 'CE';

        renderEnvironmentSelect();

        expect(screen.queryByText('DEVELOPMENT')).not.toBeInTheDocument();
    });

    it('should render the short label in the compact variant', () => {
        renderEnvironmentSelect('compact');

        expect(screen.getByText('DEV')).toBeInTheDocument();
        expect(screen.queryByText('DEVELOPMENT')).not.toBeInTheDocument();
    });

    it('should drop the label entirely in the icon variant, keeping the name accessible', () => {
        renderEnvironmentSelect('icon');

        expect(screen.queryByText('DEV')).not.toBeInTheDocument();
        expect(screen.queryByText('DEVELOPMENT')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'DEVELOPMENT'})).toBeInTheDocument();
    });

    it('should still offer the full label of every environment in the compact menu', async () => {
        renderEnvironmentSelect('compact');

        await userEvent.click(screen.getByRole('button'));

        expect(await screen.findByText('STAGING')).toBeInTheDocument();
        expect(screen.getByText('PRODUCTION')).toBeInTheDocument();
    });
});
