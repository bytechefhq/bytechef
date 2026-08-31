import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ExpressionHelpNote, {EXPRESSIONS_DOCUMENTATION_URL} from './ExpressionHelpNote';

describe('ExpressionHelpNote', () => {
    it('tells the user the field supports functions', () => {
        render(<ExpressionHelpNote />);

        expect(screen.getByText(/supports functions/)).toBeInTheDocument();
    });

    it('links to the expressions reference in a new tab', () => {
        render(<ExpressionHelpNote />);

        const link = screen.getByRole('link', {name: /Learn more/});

        expect(link).toHaveAttribute('href', EXPRESSIONS_DOCUMENTATION_URL);
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noreferrer');
    });
});
