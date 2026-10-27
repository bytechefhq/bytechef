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

const containsBooleanDefinition = {
    category: 'COLLECTION',
    description: '',
    example: '',
    name: 'contains',
    parameters: [
        {description: 'The list to search in', name: 'list', required: true, type: 'LIST'},
        {description: 'The boolean element to search for', name: 'element', required: true, type: 'BOOLEAN'},
    ],
    returnType: 'BOOLEAN',
    title: '',
} as never;

const containsMapDefinition = {
    category: 'COLLECTION',
    description: '',
    example: '',
    name: 'contains',
    parameters: [
        {description: 'The list to search in', name: 'list', required: true, type: 'LIST'},
        {description: 'The map element to search for', name: 'element', required: true, type: 'MAP'},
    ],
    returnType: 'BOOLEAN',
    title: '',
} as never;

describe('FunctionSignatureTooltip', () => {
    it('renders the function name and return type', () => {
        render(<FunctionSignatureTooltip activeArgIndex={0} definitions={[definition]} />);

        expect(screen.getByText('substring')).toBeInTheDocument();
        expect(screen.getAllByText(/String/).length).toBeGreaterThan(0);
    });

    it('shows the active parameter description', () => {
        render(<FunctionSignatureTooltip activeArgIndex={1} definitions={[definition]} />);

        expect(screen.getByText('where to start')).toBeInTheDocument();
    });

    it('renders every overload of the function', () => {
        render(
            <FunctionSignatureTooltip
                activeArgIndex={1}
                definitions={[containsBooleanDefinition, containsMapDefinition]}
            />
        );

        expect(screen.getAllByText('contains')).toHaveLength(2);
        expect(screen.getByText('element: Boolean')).toBeInTheDocument();
        expect(screen.getByText('element: Map')).toBeInTheDocument();
        expect(screen.getByText('The boolean element to search for')).toBeInTheDocument();
        expect(screen.getByText('The map element to search for')).toBeInTheDocument();
    });

    it('shows a parameter description shared by all overloads once', () => {
        render(
            <FunctionSignatureTooltip
                activeArgIndex={0}
                definitions={[containsBooleanDefinition, containsMapDefinition]}
            />
        );

        expect(screen.getAllByText('The list to search in')).toHaveLength(1);
    });
});
