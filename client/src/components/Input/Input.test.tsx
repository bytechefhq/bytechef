import '@testing-library/jest-dom';

import {render, screen, userEvent} from '../../utils/test-utils';
import Input from './Input';

describe('Input', async () => {
    it('should render the input', () => {
        render(
            <Input
                name="email"
                type="email"
                error={undefined}
                placeholder="Email"
                label="Email Address"
                aria-label="Email Address"
            />
        );
        expect(screen.getByText('Email Address')).toBeInTheDocument();
        expect(
            screen.getByRole('textbox', {
                name: /email address/i,
            })
        ).toBeInTheDocument();
    });
    it('should change input value', () => {
        render(
            <Input
                name="email"
                type="email"
                error={undefined}
                placeholder="Email"
                label="Email Address"
                aria-label="Email Address"
            />
        );

        screen.logTestingPlaygroundURL();

        const input = screen.getByRole('textbox', {
            name: /email address/i,
        });
        expect(input).toBeInTheDocument();
        userEvent.type(input, '1337');
        expect(input).toHaveValue('1337');
    });
    it('should render the input with error', () => {
        render(
            <Input
                name="email"
                type="email"
                placeholder="Email"
                label="Email Address"
                aria-label="Email Address"
                error
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
});
