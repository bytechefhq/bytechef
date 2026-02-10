import {act, fireEvent, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanelInput from '../PropertyCodeEditorDialogRightPanelInput';

const hoisted = vi.hoisted(() => {
    return {
        mockSetInputParameters: vi.fn(),
    };
});

vi.mock('../stores/usePropertyCodeEditorDialogStore', () => ({
    usePropertyCodeEditorDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            setInputParameters: hoisted.mockSetInputParameters,
        }),
}));

let mockOnChange: ((value: string | undefined) => void) | null = null;

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        onChange,
        value,
    }: {
        className?: string;
        defaultLanguage: string;
        onChange: (value: string | undefined) => void;
        onMount: (editor: {updateOptions: (options: object) => void}) => void;
        value?: string;
    }) => {
        mockOnChange = onChange;

        return (
            <div data-testid="monaco-editor">
                <span data-testid="editor-value">{value}</span>

                <button
                    data-testid="change-value-btn"
                    onClick={() => {
                        onChange('{"changed": true}');
                    }}
                >
                    Change
                </button>
            </div>
        );
    },
}));

vi.mock('@/shared/components/MonacoEditorLoader', () => ({
    default: () => <div data-testid="monaco-loader">Loading...</div>,
}));

describe('PropertyCodeEditorDialogRightPanelInput', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockOnChange = null;
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

        it('should render editor container for non-empty input', () => {
            const input = {
                name: 'John',
                value: 'test',
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByTestId('input-editor-container')).toBeInTheDocument();
        });

        it('should render editor container for nested objects', () => {
            const input = {
                item: {
                    field1: 'sample',
                    field2: 123,
                },
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByTestId('input-editor-container')).toBeInTheDocument();
        });

        it('should render editor container for numeric and boolean values', () => {
            const input = {
                count: 42,
                disabled: false,
                enabled: true,
            };

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            expect(screen.getByTestId('input-editor-container')).toBeInTheDocument();
        });
    });

    describe('reset behavior', () => {
        it('should not show reset button when editor value matches input', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            expect(screen.queryByTitle('Reset to original')).not.toBeInTheDocument();
        });

        it('should show reset button when editor value changes', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            const changeButton = await screen.findByTestId('change-value-btn');

            fireEvent.click(changeButton);

            expect(screen.getByTitle('Reset to original')).toBeInTheDocument();
        });

        it('should hide reset button and restore original value after clicking reset', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            const changeButton = await screen.findByTestId('change-value-btn');

            fireEvent.click(changeButton);

            expect(screen.getByTitle('Reset to original')).toBeInTheDocument();

            fireEvent.click(screen.getByTitle('Reset to original'));

            expect(screen.queryByTitle('Reset to original')).not.toBeInTheDocument();
            expect(screen.getByTestId('editor-value').textContent).toBe(JSON.stringify(input, null, 2));
        });
    });

    describe('JSON validation', () => {
        it('should display error for invalid JSON', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            act(() => {
                mockOnChange!('not valid json');
            });

            expect(screen.getByText('Invalid JSON')).toBeInTheDocument();
        });

        it('should display error when JSON is an array', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            act(() => {
                mockOnChange!('[1, 2, 3]');
            });

            expect(screen.getByText('JSON must be an object')).toBeInTheDocument();
        });

        it('should display error when JSON is a string', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            act(() => {
                mockOnChange!('"just a string"');
            });

            expect(screen.getByText('JSON must be an object')).toBeInTheDocument();
        });

        it('should display error when JSON is a number', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            act(() => {
                mockOnChange!('42');
            });

            expect(screen.getByText('JSON must be an object')).toBeInTheDocument();
        });

        it('should display error when JSON is null', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            act(() => {
                mockOnChange!('null');
            });

            expect(screen.getByText('JSON must be an object')).toBeInTheDocument();
        });

        it('should not update input parameters on invalid JSON', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            hoisted.mockSetInputParameters.mockClear();

            act(() => {
                mockOnChange!('not valid json');
            });

            expect(hoisted.mockSetInputParameters).not.toHaveBeenCalled();
        });

        it('should update input parameters on valid JSON object', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            hoisted.mockSetInputParameters.mockClear();

            act(() => {
                mockOnChange!('{"updated": true}');
            });

            expect(hoisted.mockSetInputParameters).toHaveBeenCalledWith({updated: true});
        });

        it('should clear error when valid JSON object is entered after invalid input', async () => {
            const input = {name: 'John'};

            render(<PropertyCodeEditorDialogRightPanelInput input={input} />);

            await screen.findByTestId('monaco-editor');

            act(() => {
                mockOnChange!('not valid json');
            });

            expect(screen.getByText('Invalid JSON')).toBeInTheDocument();

            act(() => {
                mockOnChange!('{"valid": true}');
            });

            expect(screen.queryByText('Invalid JSON')).not.toBeInTheDocument();
        });
    });
});
