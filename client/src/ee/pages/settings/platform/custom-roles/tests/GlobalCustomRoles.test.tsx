import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import GlobalCustomRoles from '../GlobalCustomRoles';

vi.mock('@/ee/shared/components/custom-roles/CustomRolesManager', () => ({
    default: () => <div data-testid="custom-roles-manager" />,
}));

describe('GlobalCustomRoles', () => {
    it('renders the manager', () => {
        render(<GlobalCustomRoles />);

        expect(screen.getByTestId('custom-roles-manager')).toBeInTheDocument();
    });
});
