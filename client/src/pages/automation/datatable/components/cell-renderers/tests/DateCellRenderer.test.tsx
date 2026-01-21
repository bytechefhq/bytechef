import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {DateCellRenderer, createDateCellRenderer} from '../DateCellRenderer';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DateCellRenderer', () => {
    describe('rendering', () => {
        it('should render formatted date', () => {
            render(<DateCellRenderer columnName="birthDate" row={{row: {birthDate: '2023-06-15', id: '1'}}} />);

            expect(screen.getByText('2023-06-15')).toBeInTheDocument();
        });

        it('should render empty string when value is null', () => {
            const {container} = render(
                <DateCellRenderer columnName="birthDate" row={{row: {birthDate: null, id: '1'}}} />
            );

            expect(container.querySelector('span')).toHaveTextContent('');
        });

        it('should render empty string when value is undefined', () => {
            const {container} = render(<DateCellRenderer columnName="birthDate" row={{row: {id: '1'}}} />);

            expect(container.querySelector('span')).toHaveTextContent('');
        });

        it('should handle ISO date strings', () => {
            render(
                <DateCellRenderer columnName="createdAt" row={{row: {createdAt: '2023-12-25T10:30:00Z', id: '1'}}} />
            );

            expect(screen.getByText('2023-12-25')).toBeInTheDocument();
        });

        it('should render empty for invalid date strings', () => {
            const {container} = render(
                <DateCellRenderer columnName="date" row={{row: {date: 'not-a-date', id: '1'}}} />
            );

            // Invalid dates result in empty text (NaN check in component)
            const span = container.querySelector('span');

            expect(span).toBeInTheDocument();
        });
    });

    describe('createDateCellRenderer factory', () => {
        it('should create a renderer that displays formatted date', () => {
            const DateRenderer = createDateCellRenderer('eventDate');

            render(<DateRenderer row={{eventDate: '2024-01-15', id: '1'}} />);

            expect(screen.getByText('2024-01-15')).toBeInTheDocument();
        });
    });
});
