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
        it('should not display no defined entries message', () => {
            const input = {
                name: 'John',
                value: 'test',
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.queryByText('No defined entries')).not.toBeInTheDocument();
        });

        it('should render an editor container for non-empty input', () => {
            const input = {
                name: 'John',
                value: 'test',
            };

            const {container} = render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            const editorContainer = container.querySelector('.min-h-0.flex-1.overflow-hidden');

            expect(editorContainer).toBeInTheDocument();
        });

        it('should render editor container for nested objects', () => {
            const input = {
                item: {
                    field1: 'sample',
                    field2: 123,
                },
            };

            const {container} = render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            const editorContainer = container.querySelector('.min-h-0.flex-1.overflow-hidden');

            expect(editorContainer).toBeInTheDocument();
        });

        it('should render editor container for numeric and boolean values', () => {
            const input = {
                count: 42,
                disabled: false,
                enabled: true,
            };

            const {container} = render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            const editorContainer = container.querySelector('.min-h-0.flex-1.overflow-hidden');

            expect(editorContainer).toBeInTheDocument();
        });
    });
});
