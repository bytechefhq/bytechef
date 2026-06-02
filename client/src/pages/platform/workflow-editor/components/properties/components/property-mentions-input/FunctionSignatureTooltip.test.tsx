import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import FunctionSignatureTooltip from './FunctionSignatureTooltip';

const definition = {
    category: 'STRING',
    description: '',
    example: '',
    name: 'substring',
    parameters: [
        {description: 'the source string', name: 'value', required: true, type: 'STRING'},
        {description: 'where to start', name: 'beginIndex', required: true, type: 'INTEGER'},
    ],
    returnType: 'STRING',
    title: '',
} as never;

describe('FunctionSignatureTooltip', () => {
    it('renders the function name and return type', () => {
        render(<FunctionSignatureTooltip activeArgIndex={0} definition={definition} />);

        expect(screen.getByText('substring')).toBeInTheDocument();
        expect(screen.getAllByText(/String/).length).toBeGreaterThan(0);
    });

    it('shows the active parameter description', () => {
        render(<FunctionSignatureTooltip activeArgIndex={1} definition={definition} />);

        expect(screen.getByText('where to start')).toBeInTheDocument();
    });
});
