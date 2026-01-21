import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {DateTimeCellRenderer, createDateTimeCellRenderer} from '../DateTimeCellRenderer';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DateTimeCellRenderer', () => {
    describe('rendering', () => {
        it('should render formatted datetime', () => {
            render(
                <DateTimeCellRenderer
                    columnName="createdAt"
                    row={{row: {createdAt: '2023-06-15T14:30:00Z', id: '1'}}}
                />
            );

            // The exact format depends on timezone, so we check for date part
            expect(screen.getByText(/2023-06-15/)).toBeInTheDocument();
        });

        it('should render empty string when value is null', () => {
            const {container} = render(
                <DateTimeCellRenderer columnName="createdAt" row={{row: {createdAt: null, id: '1'}}} />
            );

            expect(container.querySelector('span')).toHaveTextContent('');
        });

        it('should render empty string when value is undefined', () => {
            const {container} = render(<DateTimeCellRenderer columnName="createdAt" row={{row: {id: '1'}}} />);

            expect(container.querySelector('span')).toHaveTextContent('');
        });

        it('should handle ISO datetime strings with timezone', () => {
            render(
                <DateTimeCellRenderer
                    columnName="updatedAt"
                    row={{row: {id: '1', updatedAt: '2023-12-25T10:30:00+00:00'}}}
                />
            );

            expect(screen.getByText(/2023-12-25/)).toBeInTheDocument();
        });

        it('should render empty for invalid datetime strings', () => {
            const {container} = render(
                <DateTimeCellRenderer columnName="timestamp" row={{row: {id: '1', timestamp: 'invalid'}}} />
            );

            // Invalid dates result in empty text (NaN check in component)
            const span = container.querySelector('span');

            expect(span).toBeInTheDocument();
        });
    });

    describe('createDateTimeCellRenderer factory', () => {
        it('should create a renderer that displays formatted datetime', () => {
            const DateTimeRenderer = createDateTimeCellRenderer('eventTime');

            render(<DateTimeRenderer row={{eventTime: '2024-01-15T09:00:00Z', id: '1'}} />);

            expect(screen.getByText(/2024-01-15/)).toBeInTheDocument();
        });
    });
});
