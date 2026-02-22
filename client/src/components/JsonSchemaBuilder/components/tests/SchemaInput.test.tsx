import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import SchemaInput from '../SchemaInput';

describe('SchemaInput', () => {
    it('renders with default value', () => {
        render(<SchemaInput onChange={vi.fn()} />);

        expect(screen.getByRole('textbox')).toHaveValue('Untitled');
    });

    it('renders with provided value', () => {
        render(<SchemaInput onChange={vi.fn()} value="myKey" />);

        expect(screen.getByRole('textbox')).toHaveValue('myKey');
    });

    it('renders label when provided', () => {
        render(<SchemaInput label="Pill Key" onChange={vi.fn()} />);

        expect(screen.getByText('Pill Key')).toBeInTheDocument();
    });

    it('calls onChange on blur when value has changed', () => {
        const onChangeMock = vi.fn();

        render(<SchemaInput onChange={onChangeMock} value="original" />);

        const input = screen.getByRole('textbox');

        fireEvent.change(input, {target: {value: 'updated'}});
        fireEvent.blur(input);

        expect(onChangeMock).toHaveBeenCalledWith('updated');
    });

    it('does not call onChange on blur when value is unchanged', () => {
        const onChangeMock = vi.fn();

        render(<SchemaInput onChange={onChangeMock} value="original" />);

        fireEvent.blur(screen.getByRole('textbox'));

        expect(onChangeMock).not.toHaveBeenCalled();
    });

    it('calls onChange on Enter key press when value has changed', () => {
        const onChangeMock = vi.fn();

        render(<SchemaInput onChange={onChangeMock} value="original" />);

        const input = screen.getByRole('textbox');

        fireEvent.change(input, {target: {value: 'updated'}});
        fireEvent.keyPress(input, {charCode: 13, key: 'Enter'});

        expect(onChangeMock).toHaveBeenCalledWith('updated');
    });

    it('commits pending change on unmount', () => {
        const onChangeMock = vi.fn();

        const {unmount} = render(<SchemaInput onChange={onChangeMock} value="original" />);

        fireEvent.change(screen.getByRole('textbox'), {target: {value: 'pendingChange'}});

        unmount();

        expect(onChangeMock).toHaveBeenCalledWith('pendingChange');
    });

    it('does not call onChange on unmount when value is unchanged', () => {
        const onChangeMock = vi.fn();

        const {unmount} = render(<SchemaInput onChange={onChangeMock} value="original" />);

        unmount();

        expect(onChangeMock).not.toHaveBeenCalled();
    });

    it('syncs local value when prop value changes', () => {
        const {rerender} = render(<SchemaInput onChange={vi.fn()} value="first" />);

        expect(screen.getByRole('textbox')).toHaveValue('first');

        rerender(<SchemaInput onChange={vi.fn()} value="second" />);

        expect(screen.getByRole('textbox')).toHaveValue('second');
    });
});
