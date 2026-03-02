import '@testing-library/jest-dom';
import {render, screen, userEvent} from '@/shared/util/test-utils';
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
});
