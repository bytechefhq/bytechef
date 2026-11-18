import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import SettingsLayout from './SettingsLayout';

vi.mock('@/shared/layout/Header', () => ({
    default: () => null,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({leftSidebarBody}: {leftSidebarBody: ReactNode}) => <div>{leftSidebarBody}</div>,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => false,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

const renderAt = (pathname: string) =>
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <SettingsLayout />
        </MemoryRouter>
    );

describe('SettingsLayout', () => {
    beforeEach(() => {
        usePlatformTypeStore.setState({currentType: undefined});
    });

    it('shows the Embedded menu on an Organization page after coming from Embedded', () => {
        usePlatformTypeStore.setState({currentType: PlatformType.EMBEDDED});

        renderAt('/settings/users');

        expect(screen.getByText('Signing Keys')).toBeInTheDocument();
        expect(screen.queryByText('Current Workspace')).not.toBeInTheDocument();
    });

    it('shows the Automation menu on an Organization page when no mode is remembered', () => {
        renderAt('/settings/users');

        expect(screen.getByText('Current Workspace')).toBeInTheDocument();
    });

    it('shows the Automation menu under Automation settings whatever mode is remembered', () => {
        usePlatformTypeStore.setState({currentType: PlatformType.EMBEDDED});

        renderAt('/automation/settings/workspace-api-keys');

        expect(screen.getByText('Current Workspace')).toBeInTheDocument();
        expect(screen.queryByText('Signing Keys')).not.toBeInTheDocument();
    });
});
