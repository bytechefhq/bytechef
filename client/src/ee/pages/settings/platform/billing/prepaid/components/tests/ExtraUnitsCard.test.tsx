import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import ExtraUnitsCard from '../ExtraUnitsCard';

describe('ExtraUnitsCard', () => {
    it('shows available, used and purchased', () => {
        render(<ExtraUnitsCard disabled={false} extraUnitTotal={3000} extraUnitUsed={1200} onBuy={vi.fn()} />);

        expect(screen.getByText('1,800')).toBeInTheDocument();
        expect(screen.getByText(/Extra job executions available/)).toBeInTheDocument();
        expect(screen.getByText('Used 1,200')).toBeInTheDocument();
        expect(screen.getByText('Purchased 3,000')).toBeInTheDocument();
    });

    it('disables buy button when disabled', () => {
        render(<ExtraUnitsCard disabled extraUnitTotal={0} extraUnitUsed={0} onBuy={vi.fn()} />);

        expect(screen.getByRole('button', {name: 'Buy extra units'})).toBeDisabled();
    });
});
