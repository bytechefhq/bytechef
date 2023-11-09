import '@testing-library/jest-dom';
import {describe, expect, it} from 'vitest';

import {render, screen, userEvent} from '../../utils/test-utils';
import Input from './Input';

describe('Input', async () => {
    it('should render the input', () => {
        render(
            <Input
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
            <Input
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
            <Input
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
        expect(screen.getByRole('alert')).toHaveTextContent(
            'This field is required'
        );
    });
    it('should render the asterisk if input is required', () => {
        render(
            <Input
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
});
