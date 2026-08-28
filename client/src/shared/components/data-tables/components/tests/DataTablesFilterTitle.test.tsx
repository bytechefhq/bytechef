import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTablesFilterTitle from '../DataTablesFilterTitle';

const hoisted = vi.hoisted(() => {
    return {
        mockUseDataTablesFilterTitle: vi.fn(),
    };
});

vi.mock('../hooks/useDataTablesFilterTitle', () => ({
    default: hoisted.mockUseDataTablesFilterTitle,
}));

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const defaultProps = {
    allTags: [{id: '1', name: 'Tag1'}],
    tagsByTableData: [{tableId: '1', tags: [{id: '1', name: 'Tag1'}]}],
};

describe('DataTablesFilterTitle', () => {
    describe('with tag filter', () => {
        beforeEach(() => {
            hoisted.mockUseDataTablesFilterTitle.mockReturnValue({
                pageTitle: 'Important',
                tagId: '1',
            });
        });

        it('should render Filter by text', () => {
            render(<DataTablesFilterTitle {...defaultProps} />);

            expect(screen.getByText('Filter by')).toBeInTheDocument();
        });

        it('should render tag label when tagId is present', () => {
            render(<DataTablesFilterTitle {...defaultProps} />);

            expect(screen.getByText('tag:')).toBeInTheDocument();
        });

        it('should render badge with tag name', () => {
            render(<DataTablesFilterTitle {...defaultProps} />);

            expect(screen.getByText('Important')).toBeInTheDocument();
        });
    });

    describe('without tag filter', () => {
        beforeEach(() => {
            hoisted.mockUseDataTablesFilterTitle.mockReturnValue({
                pageTitle: 'Data Tables',
                tagId: null,
            });
        });

        it('should render Filter by text', () => {
            render(<DataTablesFilterTitle {...defaultProps} />);

            expect(screen.getByText('Filter by')).toBeInTheDocument();
        });

        it('should render none when no tag is selected', () => {
            render(<DataTablesFilterTitle {...defaultProps} />);

            expect(screen.getByText('none')).toBeInTheDocument();
        });

        it('should not render tag label', () => {
            render(<DataTablesFilterTitle {...defaultProps} />);

            expect(screen.queryByText('tag:')).not.toBeInTheDocument();
        });
    });

    describe('with undefined pageTitle', () => {
        beforeEach(() => {
            hoisted.mockUseDataTablesFilterTitle.mockReturnValue({
                pageTitle: undefined,
                tagId: '1',
            });
        });

        it('should render Unknown Tag as fallback', () => {
            render(<DataTablesFilterTitle {...defaultProps} />);

            expect(screen.getByText('Unknown Tag')).toBeInTheDocument();
        });
    });
});
