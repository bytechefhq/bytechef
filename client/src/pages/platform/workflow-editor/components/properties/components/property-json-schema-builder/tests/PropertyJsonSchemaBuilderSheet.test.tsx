import {useResponseSchemaNoteStore} from '@/pages/platform/workflow-editor/stores/useResponseSchemaNoteStore';
import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyJsonSchemaBuilderSheet from '../PropertyJsonSchemaBuilderSheet';

vi.mock('@/components/JsonSchemaBuilder/JsonSchemaBuilder', () => ({
    default: () => <div data-testid="json-schema-builder" />,
}));

vi.mock('@/shared/components/MonacoEditorLoader', () => ({
    default: () => <div data-testid="monaco-loader" />,
}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: () => <div data-testid="monaco-editor" />,
}));

const NOTE_TEXT = /Define how you'd like the LLM to structure its responses/;

beforeEach(() => {
    windowResizeObserver();

    localStorage.clear();

    useResponseSchemaNoteStore.setState({showResponseSchemaNote: true});
});

afterEach(() => {
    resetAll();
});

describe('PropertyJsonSchemaBuilderSheet', () => {
    it('shows the Response Schema hint note until it is dismissed', async () => {
        render(<PropertyJsonSchemaBuilderSheet title="Response Schema" />);

        const noteText = screen.getByText(NOTE_TEXT);

        await userEvent.click(within(noteText.parentElement!).getByRole('button', {name: 'Close'}));

        expect(screen.queryByText(NOTE_TEXT)).not.toBeInTheDocument();
        expect(useResponseSchemaNoteStore.getState().showResponseSchemaNote).toBe(false);
    });

    it('keeps the hint note hidden when the sheet is opened again after a dismissal', () => {
        useResponseSchemaNoteStore.setState({showResponseSchemaNote: false});

        render(<PropertyJsonSchemaBuilderSheet title="Response Schema" />);

        expect(screen.queryByText(NOTE_TEXT)).not.toBeInTheDocument();
    });

    it('does not show the hint note for other schemas', () => {
        render(<PropertyJsonSchemaBuilderSheet title="Output Schema" />);

        expect(screen.queryByText(NOTE_TEXT)).not.toBeInTheDocument();
    });
});
