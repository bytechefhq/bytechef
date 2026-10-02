import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyJsonSchemaBuilderSampleDataDialog from '../PropertyJsonSchemaBuilderSampleDataDialog';

vi.mock('@/shared/components/MonacoEditorLoader', () => ({
    default: () => <div data-testid="monaco-loader" />,
}));

vi.mock('@monaco-editor/react', () => ({
    default: () => <div data-testid="monaco-editor" />,
}));

const onChange = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('PropertyJsonSchemaBuilderSampleDataDialog', () => {
    describe('trigger', () => {
        it('should render the Generate trigger', () => {
            render(<PropertyJsonSchemaBuilderSampleDataDialog onChange={onChange} />);

            expect(screen.getByRole('button', {name: 'Generate'})).toBeInTheDocument();
        });
    });

    describe('dialog', () => {
        it('should open with the title and description', async () => {
            const user = userEvent.setup();

            render(<PropertyJsonSchemaBuilderSampleDataDialog onChange={onChange} />);

            await user.click(screen.getByRole('button', {name: 'Generate'}));

            expect(await screen.findByText('Sample JSON')).toBeInTheDocument();
            expect(screen.getByText('Generate JSON schema from sample JSON')).toBeInTheDocument();
        });

        it('should render the close control', async () => {
            const user = userEvent.setup();

            render(<PropertyJsonSchemaBuilderSampleDataDialog onChange={onChange} />);

            await user.click(screen.getByRole('button', {name: 'Generate'}));

            expect(await screen.findByRole('button', {name: 'Close'})).toBeInTheDocument();
        });

        it('should disable the generate action until a schema is parsed', async () => {
            const user = userEvent.setup();

            render(<PropertyJsonSchemaBuilderSampleDataDialog onChange={onChange} />);

            await user.click(screen.getByRole('button', {name: 'Generate'}));

            const dialog = await screen.findByRole('dialog');

            expect(within(dialog).getByRole('button', {name: 'Generate'})).toBeDisabled();
        });

        it('should close when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<PropertyJsonSchemaBuilderSampleDataDialog onChange={onChange} />);

            await user.click(screen.getByRole('button', {name: 'Generate'}));
            await user.click(await screen.findByRole('button', {name: 'Close'}));

            expect(screen.queryByText('Sample JSON')).not.toBeInTheDocument();
        });
    });
});
