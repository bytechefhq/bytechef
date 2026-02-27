import {TooltipProvider} from '@/components/ui/tooltip';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import {mockScrollIntoView, render, screen} from '@/shared/util/test-utils';
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

const renderEnvironmentSelect = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <EnvironmentSelect />
            </TooltipProvider>
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
});
