import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import AppOverviewTab from '../components/AppOverviewTab';

const app = {
    description: null,
    foreignInstances: [{id: '13'}],
    id: '1',
    instances: [{id: '11'}, {id: '12'}],
    name: 'crm-dashboard',
    ownerUserId: '42',
    status: 'SUSPENDED',
} as never;

describe('AppOverviewTab', () => {
    it('shows the app status and the owner name', () => {
        render(<AppOverviewTab app={app} ownerName="Ada Lovelace" />);

        expect(screen.getByText('Status')).toBeInTheDocument();
        expect(screen.getByText('Suspended')).toBeInTheDocument();
        expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
        expect(screen.getByText('3')).toBeInTheDocument();
        expect(screen.queryByText('Key hint')).not.toBeInTheDocument();
    });
});
