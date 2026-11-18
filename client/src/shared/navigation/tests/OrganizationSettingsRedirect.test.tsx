import OrganizationSettingsRedirect from '@/shared/navigation/OrganizationSettingsRedirect';
import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

const LocationProbe = () => {
    const location = useLocation();

    return <div>{`${location.pathname}${location.search}${location.hash}`}</div>;
};

const renderAt = (initialEntry: string) =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route element={<OrganizationSettingsRedirect />} path="/automation/settings/*" />

                <Route element={<OrganizationSettingsRedirect />} path="/embedded/settings/*" />

                <Route element={<LocationProbe />} path="/settings/*" />
            </Routes>
        </MemoryRouter>
    );

describe('OrganizationSettingsRedirect', () => {
    it('moves an old Automation Organization page to /settings with its query and fragment', () => {
        renderAt('/automation/settings/notifications?id=4#body-template');

        expect(screen.getByText('/settings/notifications?id=4#body-template')).toBeInTheDocument();
    });

    it('moves a nested old Embedded Organization page to /settings', () => {
        renderAt('/embedded/settings/ai/skills/42');

        expect(screen.getByText('/settings/ai/skills/42')).toBeInTheDocument();
    });
});
