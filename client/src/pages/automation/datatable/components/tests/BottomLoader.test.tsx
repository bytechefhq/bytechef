import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import BottomLoader from '../BottomLoader';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('BottomLoader', () => {
    describe('rendering', () => {
        it('should render total row count', () => {
            render(<BottomLoader isFetchingNextPage={false} rowCount={100} />);

            expect(screen.getByText('Total rows: 100')).toBeInTheDocument();
        });

        it('should render row count of 0', () => {
            render(<BottomLoader isFetchingNextPage={false} rowCount={0} />);

            expect(screen.getByText('Total rows: 0')).toBeInTheDocument();
        });

        it('should render large row count', () => {
            render(<BottomLoader isFetchingNextPage={false} rowCount={10000} />);

            expect(screen.getByText('Total rows: 10000')).toBeInTheDocument();
        });
    });

    describe('loading state', () => {
        it('should show loading text when fetching next page', () => {
            render(<BottomLoader isFetchingNextPage={true} rowCount={50} />);

            expect(screen.getByText('Loading…')).toBeInTheDocument();
        });

        it('should not show loading text when not fetching', () => {
            render(<BottomLoader isFetchingNextPage={false} rowCount={50} />);

            expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
        });

        it('should show both row count and loading text when fetching', () => {
            render(<BottomLoader isFetchingNextPage={true} rowCount={75} />);

            expect(screen.getByText('Total rows: 75')).toBeInTheDocument();
            expect(screen.getByText('Loading…')).toBeInTheDocument();
        });
    });
});
