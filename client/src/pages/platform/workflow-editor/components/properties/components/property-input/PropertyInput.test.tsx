import '@testing-library/jest-dom';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {useRef, useState} from 'react';
import {describe, expect, it, vi} from 'vitest';

import PropertyInput from './PropertyInput';

describe('PropertyInput', async () => {
    it('should render the input', () => {
        render(
            <PropertyInput
                aria-label="Email Address"
                error={undefined}
                label="Email Address"
                name="email"
                placeholder="Email"
                type="email"
            />
        );

        expect(screen.getByText('Email Address')).toBeInTheDocument();

        expect(
            screen.getByRole('textbox', {
                name: /email address/i,
            })
        ).toBeInTheDocument();
    });

    it('should change input value', async () => {
        render(
            <PropertyInput
                aria-label="Email Address"
                label="Email Address"
                name="email"
                placeholder="Email"
                type="email"
            />
        );

        screen.logTestingPlaygroundURL();

        const input = screen.getByRole('textbox', {
            name: /email address/i,
        });

        expect(input).toBeInTheDocument();

        await userEvent.type(input, 'foo@bar.com');

        expect(input).toHaveValue('foo@bar.com');
    });

    it('should render the input with error', () => {
        render(
            <PropertyInput
                aria-label="Email Address"
                error
                label="Email Address"
                name="email"
                placeholder="Email"
                type="email"
            />
        );

        expect(
            screen.getByRole('textbox', {
                name: /email address/i,
            })
        ).toBeInTheDocument();

        expect(screen.getByRole('alert')).toHaveTextContent('This field is required');
    });

    it('should render the asterisk if input is required', () => {
        render(
            <PropertyInput
                aria-label="Email Address"
                error={undefined}
                label="Email Address"
                name="email"
                placeholder="Email"
                required
                type="email"
            />
        );

        expect(screen.getByText('Email Address')).toBeInTheDocument();

        expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('renders trailingAction when provided', () => {
        render(
            <PropertyInput
                aria-label="Field"
                label="Field"
                name="field"
                trailingAction={<button data-testid="trailing-action">Action</button>}
            />
        );

        expect(screen.getByTestId('trailing-action')).toBeInTheDocument();
    });

    it('invokes trailingAction click handler (covers the TIME clear button pattern from #4768)', async () => {
        const handleClear = vi.fn();

        render(
            <PropertyInput
                aria-label="Time"
                label="Time"
                name="time"
                trailingAction={
                    <button aria-label="Clear time" onClick={handleClear} type="button">
                        X
                    </button>
                }
                type="time"
                value="12:30"
            />
        );

        await userEvent.click(screen.getByRole('button', {name: /clear time/i}));

        expect(handleClear).toHaveBeenCalledTimes(1);
    });

    it('strips leading = from display value on input change when expressionPrefix is true', async () => {
        const handleChange = vi.fn();

        render(
            <PropertyInput
                aria-label="Expression"
                expressionPrefix
                label="Expression"
                name="expr"
                onChange={handleChange}
            />
        );

        const input = screen.getByRole('textbox', {name: /expression/i});

        await userEvent.clear(input);
        await userEvent.type(input, '=someExpression');

        // The handleInputChange strips the leading = when expressionPrefix is true
        expect(input).toHaveValue('someExpression');
    });

    it('does not strip = from display when expressionPrefix is false', () => {
        render(<PropertyInput aria-label="Normal" label="Normal" name="normal" value="=someValue" />);

        const input = screen.getByRole('textbox', {name: /normal/i});

        expect(input).toHaveValue('=someValue');
    });

    it('uses minute precision (step=60) for time inputs so the field is easily clearable', () => {
        render(<PropertyInput aria-label="Time" label="Time" name="time" type="time" />);

        const input = screen.getByLabelText(/time/i);

        expect(input).toHaveAttribute('step', '60');
    });

    it('keeps step=1 for non-time inputs', () => {
        render(<PropertyInput aria-label="Date" label="Date" name="date" type="date" />);

        const input = screen.getByLabelText(/date/i);

        expect(input).toHaveAttribute('step', '1');
    });

    // Regression for #4768: clearing the TIME field used to call inputRef.focus() synchronously
    // after setInputValue(''). The focus flipped PropertyInput's isFocused to true inside the same
    // batch, so the value-sync useEffect skipped, localValue stayed at the old time, and the input
    // only visually cleared after the next blur. The fix defers focus() via requestAnimationFrame
    // so value='' lands in a render where isFocused is still false.
    it('clears the displayed value when the parent sets value to "" and defers focus via rAF', async () => {
        const Harness = () => {
            const [value, setValue] = useState('12:30');
            const inputRef = useRef<HTMLInputElement>(null);

            const handleClear = () => {
                setValue('');

                requestAnimationFrame(() => inputRef.current?.focus());
            };

            return (
                <PropertyInput
                    aria-label="Time"
                    label="Time"
                    name="time"
                    ref={inputRef}
                    trailingAction={
                        <button aria-label="Clear time" onClick={handleClear} type="button">
                            X
                        </button>
                    }
                    type="time"
                    value={value}
                />
            );
        };

        const {container} = render(<Harness />);

        const input = container.querySelector('input[name="time"]') as HTMLInputElement;

        expect(input.value).toBe('12:30');

        await userEvent.click(screen.getByRole('button', {name: /clear time/i}));

        await waitFor(() => expect(input.value).toBe(''));
    });
});
