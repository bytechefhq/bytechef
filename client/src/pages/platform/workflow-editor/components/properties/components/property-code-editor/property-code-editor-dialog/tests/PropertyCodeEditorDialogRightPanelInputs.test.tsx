import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanelInputs from '../PropertyCodeEditorDialogRightPanelInputs';

describe('PropertyCodeEditorDialogRightPanelInputs', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the Inputs title', () => {
            render(<PropertyCodeEditorDialogRightPanelInputs input={{}} />);

            expect(screen.getByText('Inputs')).toBeInTheDocument();
        });
    });

    describe('when input is empty', () => {
        it('should display no defined entries message', () => {
            render(<PropertyCodeEditorDialogRightPanelInputs input={{}} />);

            expect(screen.getByText('No defined entries')).toBeInTheDocument();
        });
    });

    describe('when input has entries', () => {
        it('should display key-value pairs', () => {
            const input = {
                name: 'John' as unknown as object,
                value: 'test' as unknown as object,
            };

            render(<PropertyCodeEditorDialogRightPanelInputs input={input} />);

            expect(screen.getByText('name')).toBeInTheDocument();
            expect(screen.getByText('John')).toBeInTheDocument();
            expect(screen.getByText('value')).toBeInTheDocument();
            expect(screen.getByText('test')).toBeInTheDocument();
        });

        it('should truncate long values', () => {
            const input = {
                longValue: 'This is a very long string that should be truncated' as unknown as object,
            };

            render(<PropertyCodeEditorDialogRightPanelInputs input={input} />);

            expect(screen.getByText('longValue')).toBeInTheDocument();
            // String is truncated at 23 characters + '...'
            expect(screen.getByText('This is a very long str...')).toBeInTheDocument();
        });

        it('should not truncate short values', () => {
            const input = {
                shortValue: 'Short' as unknown as object,
            };

            render(<PropertyCodeEditorDialogRightPanelInputs input={input} />);

            expect(screen.getByText('Short')).toBeInTheDocument();
        });

        it('should display multiple entries', () => {
            const input = {
                key1: 'value1' as unknown as object,
                key2: 'value2' as unknown as object,
                key3: 'value3' as unknown as object,
            };

            render(<PropertyCodeEditorDialogRightPanelInputs input={input} />);

            expect(screen.getByText('key1')).toBeInTheDocument();
            expect(screen.getByText('value1')).toBeInTheDocument();
            expect(screen.getByText('key2')).toBeInTheDocument();
            expect(screen.getByText('value2')).toBeInTheDocument();
            expect(screen.getByText('key3')).toBeInTheDocument();
            expect(screen.getByText('value3')).toBeInTheDocument();
        });
    });

    describe('value truncation edge cases', () => {
        it('should handle exactly 23 character values', () => {
            const input = {
                exact: '12345678901234567890123' as unknown as object, // 23 chars
            };

            render(<PropertyCodeEditorDialogRightPanelInputs input={input} />);

            expect(screen.getByText('12345678901234567890123')).toBeInTheDocument();
        });

        it('should truncate 24 character values', () => {
            const input = {
                overLimit: '123456789012345678901234' as unknown as object, // 24 chars
            };

            render(<PropertyCodeEditorDialogRightPanelInputs input={input} />);

            expect(screen.getByText('12345678901234567890123...')).toBeInTheDocument();
        });
    });
});
