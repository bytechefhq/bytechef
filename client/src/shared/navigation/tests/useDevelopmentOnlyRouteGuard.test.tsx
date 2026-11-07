import {useDevelopmentOnlyRouteGuard} from '@/shared/navigation/useDevelopmentOnlyRouteGuard';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({currentEnvironmentId: 0}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({currentEnvironmentId: hoisted.currentEnvironmentId}),
}));

const LocationProbe = () => {
    const {pathname} = useLocation();

    useDevelopmentOnlyRouteGuard();

    return <div data-testid="pathname">{pathname}</div>;
};

const renderAt = (initialEntry: string) =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route element={<LocationProbe />} path="*" />
            </Routes>
        </MemoryRouter>
    );

describe('useDevelopmentOnlyRouteGuard', () => {
    beforeEach(() => {
        hoisted.currentEnvironmentId = 0;
    });

    it('stays put in the development environment', () => {
        renderAt('/automation/projects');

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/projects');
    });

    it('redirects a development-only list page in production', () => {
        hoisted.currentEnvironmentId = 2;

        renderAt('/automation/projects');

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/deployments');
    });

    it('redirects a development-only detail page in staging', () => {
        hoisted.currentEnvironmentId = 1;

        renderAt('/automation/projects/12/project-workflows/34');

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/deployments');
    });

    it('redirects the embedded integrations to their configurations', () => {
        hoisted.currentEnvironmentId = 1;

        renderAt('/embedded/integrations/12/integration-workflows/34');

        expect(screen.getByTestId('pathname')).toHaveTextContent('/embedded/configurations');
    });

    it('redirects the embedded automation workflows to their configurations', () => {
        hoisted.currentEnvironmentId = 1;

        renderAt('/embedded/automation-workflows/12/editor');

        expect(screen.getByTestId('pathname')).toHaveTextContent('/embedded/automation-configurations');
    });

    it('leaves a deployed surface alone in production', () => {
        hoisted.currentEnvironmentId = 2;

        renderAt('/automation/deployments');

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/deployments');
    });

    it('redirects when the environment changes under a page that was allowed', () => {
        const {rerender} = renderAt('/automation/projects');

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/projects');

        hoisted.currentEnvironmentId = 2;

        rerender(
            <MemoryRouter initialEntries={['/automation/projects']}>
                <Routes>
                    <Route element={<LocationProbe />} path="*" />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/deployments');
    });
});
