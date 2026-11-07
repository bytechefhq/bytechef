import {useResponseSchemaNoteStore} from '@/pages/platform/workflow-editor/stores/useResponseSchemaNoteStore';
import {i18n} from '@lingui/core';
import {I18nProvider} from '@lingui/react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';

const {generateSchemaFromSampleMock} = vi.hoisted(() => ({
    generateSchemaFromSampleMock: vi.fn(),
}));

vi.mock('./utils/generateSchemaFromSample', () => ({
    generateSchemaFromSample: generateSchemaFromSampleMock,
}));
vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({onChange, value}: {onChange: (value: string | undefined) => void; value: string}) => (
        <textarea data-testid="mock-monaco-editor" onChange={(event) => onChange(event.target.value)} value={value} />
    ),
}));
vi.mock('@/components/JsonSchemaBuilder/JsonSchemaBuilder', () => ({
    default: () => <div data-testid="json-schema-builder" />,
}));

import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyJsonSchemaBuilderSheet from './PropertyJsonSchemaBuilderSheet';

i18n.load('en', {});
i18n.activate('en');

const wrapper = ({children}: {children: ReactNode}) => <I18nProvider i18n={i18n}>{children}</I18nProvider>;

const generatedSchema = {
    properties: {name: {type: 'string'}},
    type: 'object',
};

describe('PropertyJsonSchemaBuilderSheet sample generation', () => {
    it('offers a From Sample button', () => {
        render(<PropertyJsonSchemaBuilderSheet title="Response Schema" />, {wrapper});

        expect(screen.getByRole('button', {name: /from sample/i})).toBeInTheDocument();
    });

    it('applies the generated schema and closes the dialog', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        generateSchemaFromSampleMock.mockResolvedValue(generatedSchema);

        render(<PropertyJsonSchemaBuilderSheet onChange={onChange} title="Response Schema" />, {wrapper});

        await user.click(screen.getByRole('button', {name: /from sample/i}));

        const dialog = await screen.findByRole('dialog', {name: /generate schema from sample/i});

        fireEvent.change(await within(dialog).findByTestId('mock-monaco-editor'), {
            target: {value: '{"name": "Ana"}'},
        });

        await user.click(within(dialog).getByRole('button', {name: /generate/i}));

        await waitFor(() => expect(onChange).toHaveBeenCalledWith(generatedSchema));

        await waitFor(() =>
            expect(screen.queryByRole('dialog', {name: /generate schema from sample/i})).not.toBeInTheDocument()
        );
    });
});

describe('PropertyJsonSchemaBuilderSheet note', () => {
    beforeEach(() => {
        useResponseSchemaNoteStore.setState({responseSchemaNoteDismissed: false});
    });

    it('hides the response schema note once it is dismissed', async () => {
        const user = userEvent.setup();

        render(<PropertyJsonSchemaBuilderSheet title="Response Schema" />, {wrapper});

        const note = screen.getByText(/essentially a template for its output/i);

        await user.click(screen.getByRole('button', {name: /dismiss/i}));

        expect(note).not.toBeInTheDocument();
    });

    it('keeps the response schema note hidden when the sheet is opened again', async () => {
        const user = userEvent.setup();

        const {unmount} = render(<PropertyJsonSchemaBuilderSheet title="Response Schema" />, {wrapper});

        await user.click(screen.getByRole('button', {name: /dismiss/i}));

        unmount();

        render(<PropertyJsonSchemaBuilderSheet title="Response Schema" />, {wrapper});

        expect(screen.queryByText(/essentially a template for its output/i)).not.toBeInTheDocument();
    });
});
