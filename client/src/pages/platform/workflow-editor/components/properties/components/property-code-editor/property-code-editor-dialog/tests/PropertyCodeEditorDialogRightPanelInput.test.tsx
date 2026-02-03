import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanelInput from '../PropertyCodeEditorDialogRightPanelInput';

describe('PropertyCodeEditorDialogRightPanelInput', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the Input title', () => {
            render(<PropertyCodeEditorDialogRightPanelInput input={{}} />);

            expect(screen.getByText('Input')).toBeInTheDocument();
        });
    });

    describe('when input is empty', () => {
        it('should display no defined entries message', () => {
            render(<PropertyCodeEditorDialogRightPanelInput input={{}} />);

            expect(screen.getByText('No defined entries')).toBeInTheDocument();
        });
    });

    describe('when input has entries', () => {
        it('should display key-value pairs', () => {
            const input = {
                name: 'John',
                value: 'test',
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('name')).toBeInTheDocument();
            expect(screen.getByText('John')).toBeInTheDocument();
            expect(screen.getByText('value')).toBeInTheDocument();
            expect(screen.getByText('test')).toBeInTheDocument();
        });

        it('should truncate long values', () => {
            const input = {
                longValue: 'This is a very long string that should be truncated',
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('longValue')).toBeInTheDocument();
            // String is truncated at 23 characters + '...'
            expect(screen.getByText('This is a very long str...')).toBeInTheDocument();
        });

        it('should not truncate short values', () => {
            const input = {
                shortValue: 'Short',
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('Short')).toBeInTheDocument();
        });

        it('should display multiple entries', () => {
            const input = {
                key1: 'value1',
                key2: 'value2',
                key3: 'value3',
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('key1')).toBeInTheDocument();
            expect(screen.getByText('value1')).toBeInTheDocument();
            expect(screen.getByText('key2')).toBeInTheDocument();
            expect(screen.getByText('value2')).toBeInTheDocument();
            expect(screen.getByText('key3')).toBeInTheDocument();
            expect(screen.getByText('value3')).toBeInTheDocument();
        });

        it('should display nested objects', () => {
            const input = {
                item: {
                    field1: 'sample',
                    field2: 123,
                },
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('item')).toBeInTheDocument();
            expect(screen.getByText('field1')).toBeInTheDocument();
            expect(screen.getByText('sample')).toBeInTheDocument();
            expect(screen.getByText('field2')).toBeInTheDocument();
            expect(screen.getByText('123')).toBeInTheDocument();
        });

        it('should display numeric and boolean values', () => {
            const input = {
                count: 42,
                disabled: false,
                enabled: true,
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('count')).toBeInTheDocument();
            expect(screen.getByText('42')).toBeInTheDocument();
            expect(screen.getByText('disabled')).toBeInTheDocument();
            expect(screen.getByText('false')).toBeInTheDocument();
            expect(screen.getByText('enabled')).toBeInTheDocument();
            expect(screen.getByText('true')).toBeInTheDocument();
        });
    });

    describe('value truncation edge cases', () => {
        it('should handle exactly 23 character values', () => {
            const input = {
                exact: '12345678901234567890123', // 23 chars
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('12345678901234567890123')).toBeInTheDocument();
        });

        it('should truncate 24 character values', () => {
            const input = {
                overLimit: '123456789012345678901234', // 24 chars
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByText('12345678901234567890123...')).toBeInTheDocument();
        });
    });
});
