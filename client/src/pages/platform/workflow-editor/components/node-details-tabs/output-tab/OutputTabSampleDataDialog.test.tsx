import {fireEvent, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import OutputTabSampleDataDialog from './OutputTabSampleDataDialog';

// Mock the lazy-loaded Monaco Editor
vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        onChange,
        onMount,
        value,
    }: {
        onChange: (value: string | undefined) => void;
        onMount: (editor: {focus: () => void}) => void;
        value: string;
    }) => {
        // Call onMount with a mock editor on first render
        if (onMount) {
            setTimeout(() => onMount({focus: vi.fn()}), 0);
        }

        return (
            <textarea
                data-testid="mock-monaco-editor"
                onChange={(event) => onChange(event.target.value)}
                value={value}
            />
        );
    },
}));

const mockOnClose = vi.fn();
const mockOnUpload = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderDialog = (props: Partial<Parameters<typeof OutputTabSampleDataDialog>[0]> = {}) => {
    return render(<OutputTabSampleDataDialog onClose={mockOnClose} onUpload={mockOnUpload} open={true} {...props} />);
};

describe('OutputTabSampleDataDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open is true', async () => {
            renderDialog();

            expect(await screen.findByRole('dialog')).toBeInTheDocument();
        });

        it('should not render the dialog when open is false', () => {
            renderDialog({open: false});

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('should render the dialog title', async () => {
            renderDialog();

            expect(await screen.findByText('Upload Sample Output Data')).toBeInTheDocument();
        });

        it('should render the dialog description', async () => {
            renderDialog();

            expect(
                await screen.findByText("Add sample value in JSON format. Click Upload when you're done.")
            ).toBeInTheDocument();
        });

        it('should render the Upload button', async () => {
            renderDialog();

            expect(await screen.findByRole('button', {name: 'Upload'})).toBeInTheDocument();
        });

        it('should render the Monaco editor', async () => {
            renderDialog();

            expect(await screen.findByTestId('mock-monaco-editor')).toBeInTheDocument();
        });
    });

    describe('Upload button state', () => {
        it('should disable Upload button when editor is empty', async () => {
            renderDialog();

            expect(await screen.findByRole('button', {name: 'Upload'})).toBeDisabled();
        });

        it('should disable Upload button when JSON is invalid', async () => {
            const user = userEvent.setup();

            renderDialog();

            const editor = await screen.findByTestId('mock-monaco-editor');

            await user.clear(editor);
            await user.type(editor, 'invalid json');

            expect(screen.getByRole('button', {name: 'Upload'})).toBeDisabled();
        });

        it('should enable Upload button when valid JSON is entered', async () => {
            renderDialog();

            const editor = await screen.findByTestId('mock-monaco-editor');

            fireEvent.change(editor, {target: {value: '{"name": "test"}'}});

            expect(screen.getByRole('button', {name: 'Upload'})).toBeEnabled();
        });
    });

    describe('placeholder prop', () => {
        it('should display placeholder value in editor when provided', async () => {
            const placeholder = {key: 'value'};

            renderDialog({placeholder});

            const editor = (await screen.findByTestId('mock-monaco-editor')) as HTMLTextAreaElement;

            expect(editor.value).toContain('"key"');
            expect(editor.value).toContain('"value"');
        });

        it('should enable Upload button when placeholder contains valid data', async () => {
            const placeholder = {key: 'value'};

            renderDialog({placeholder});

            expect(await screen.findByRole('button', {name: 'Upload'})).toBeEnabled();
        });

        it('should not display placeholder when empty object is provided', async () => {
            const placeholder = {};

            renderDialog({placeholder});

            const editor = (await screen.findByTestId('mock-monaco-editor')) as HTMLTextAreaElement;

            expect(editor.value).toBe('');
        });
    });

    describe('upload functionality', () => {
        it('should call onUpload with stringified JSON when Upload is clicked', async () => {
            const user = userEvent.setup();

            renderDialog();

            const editor = await screen.findByTestId('mock-monaco-editor');

            fireEvent.change(editor, {target: {value: '{"test": 123}'}});

            const uploadButton = screen.getByRole('button', {name: 'Upload'});

            await user.click(uploadButton);

            expect(mockOnUpload).toHaveBeenCalledWith('{"test":123}');
        });

        it('should not call onUpload when Upload is clicked with invalid JSON', async () => {
            const user = userEvent.setup();

            renderDialog();

            const editor = await screen.findByTestId('mock-monaco-editor');

            await user.clear(editor);
            await user.type(editor, 'not json');

            const uploadButton = screen.getByRole('button', {name: 'Upload'});

            // Button should be disabled, so onUpload shouldn't be called
            expect(uploadButton).toBeDisabled();
            expect(mockOnUpload).not.toHaveBeenCalled();
        });
    });

    describe('cursor stability (raw value preservation)', () => {
        it('should preserve the raw input value without reformatting', async () => {
            renderDialog();

            const editor = (await screen.findByTestId('mock-monaco-editor')) as HTMLTextAreaElement;

            // Type compact JSON without formatting
            const compactJson = '{"a":1}';

            fireEvent.change(editor, {target: {value: compactJson}});

            // The value should remain as typed, not reformatted
            expect(editor.value).toBe(compactJson);
        });
    });

    describe('dialog close behavior', () => {
        it('should call onClose when dialog is closed', async () => {
            const user = userEvent.setup();

            renderDialog();

            // Find and click the close button
            const closeButton = await screen.findByRole('button', {name: 'Close'});

            await user.click(closeButton);

            expect(mockOnClose).toHaveBeenCalled();
        });
    });
});
