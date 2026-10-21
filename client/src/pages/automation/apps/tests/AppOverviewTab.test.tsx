import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import AppOverviewTab from '../components/AppOverviewTab';

const app = {
    description: null,
    hostingTarget: 'VERCEL',
    id: '1',
    name: 'crm-dashboard',
    ownerUserId: '42',
    secretKeyHint: 'ijklmnop',
    status: 'DISABLED',
    url: null,
} as never;

describe('AppOverviewTab', () => {
    it('shows the app status and the owner name', () => {
        render(<AppOverviewTab app={app} ownerName="Ada Lovelace" />);

        expect(screen.getByText('Status')).toBeInTheDocument();
        expect(screen.getByText('Disabled')).toBeInTheDocument();
        expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
    });
});
