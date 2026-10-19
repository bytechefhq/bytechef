import {render, resetAll, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import AccountProfile from '../AccountProfile';

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {title: string}) => <h1>{title}</h1>,
}));

vi.mock('@/pages/account/settings/AccountProfileDetails', () => ({
    default: () => <section>details</section>,
}));

vi.mock('@/pages/account/settings/AccountProfilePassword', () => ({
    default: () => <section>password</section>,
}));

vi.mock('@/pages/account/settings/AccountProfileMfa', () => ({
    default: () => <section>mfa</section>,
}));

vi.mock('@/pages/account/settings/AccountProfileLinkedAccounts', () => ({
    default: () => <section>linked accounts</section>,
}));

vi.mock('@/pages/account/settings/AccountProfileDeleteOrganization', () => ({
    default: () => <section>delete organization</section>,
}));

afterEach(() => {
    resetAll();
});

describe('AccountProfile', () => {
    it('renders the delete organization section after the other sections', () => {
        render(<AccountProfile />);

        const sections = screen.getAllByText(/details|password|delete organization/);

        expect(screen.getByRole('heading', {name: 'Your profile'})).toBeInTheDocument();
        expect(sections.at(-1)).toHaveTextContent('delete organization');
    });
});
