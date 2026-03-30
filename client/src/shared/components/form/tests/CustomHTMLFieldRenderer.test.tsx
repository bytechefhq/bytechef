import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {CustomHTMLFieldRenderer} from '../CustomHTMLFieldRenderer';
import {createMockForm} from './testUtils';

describe('CustomHTMLFieldRenderer', () => {
    it('should render field description when provided', () => {
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{fieldDescription: 'Custom HTML section'}} />, {wrapper});

        expect(screen.getByText('Custom HTML section')).toBeInTheDocument();
    });

    it('should not render field description when not provided', () => {
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{}} />, {wrapper});

        expect(screen.queryByText('Custom HTML section')).not.toBeInTheDocument();
    });

    it('should render sanitized HTML from defaultValue', () => {
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{defaultValue: '<p>Hello World</p>'}} />, {wrapper});

        expect(screen.getByText('Hello World')).toBeInTheDocument();
    });

    it('should allow safe HTML tags', () => {
        const safeHtml = '<h1>Title</h1><p>Paragraph</p><strong>Bold</strong><em>Italic</em>';
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{defaultValue: safeHtml}} />, {wrapper});

        expect(screen.getByText('Title')).toBeInTheDocument();
        expect(screen.getByText('Paragraph')).toBeInTheDocument();
        expect(screen.getByText('Bold')).toBeInTheDocument();
        expect(screen.getByText('Italic')).toBeInTheDocument();
    });

    it('should render lists correctly', () => {
        const listHtml = '<ul><li>Item 1</li><li>Item 2</li></ul>';
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{defaultValue: listHtml}} />, {wrapper});

        expect(screen.getByText('Item 1')).toBeInTheDocument();
        expect(screen.getByText('Item 2')).toBeInTheDocument();
    });

    it('should render ordered lists correctly', () => {
        const listHtml = '<ol><li>First</li><li>Second</li></ol>';
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{defaultValue: listHtml}} />, {wrapper});

        expect(screen.getByText('First')).toBeInTheDocument();
        expect(screen.getByText('Second')).toBeInTheDocument();
    });

    it('should strip disallowed HTML tags', () => {
        const unsafeHtml = '<script>alert("xss")</script><p>Safe content</p>';
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{defaultValue: unsafeHtml}} />, {wrapper});

        expect(screen.getByText('Safe content')).toBeInTheDocument();
        expect(screen.queryByText('alert("xss")')).not.toBeInTheDocument();
    });

    it('should strip anchor tags', () => {
        const htmlWithLink = '<a href="https://example.com">Click me</a><p>Other content</p>';
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{defaultValue: htmlWithLink}} />, {wrapper});

        expect(screen.queryByRole('link')).not.toBeInTheDocument();
        expect(screen.getByText('Other content')).toBeInTheDocument();
    });

    it('should render empty content when defaultValue is undefined', () => {
        const {wrapper} = createMockForm();

        const {container} = render(<CustomHTMLFieldRenderer formInput={{}} />, {wrapper});

        const proseDiv = container.querySelector('.prose');
        expect(proseDiv).toBeInTheDocument();
        expect(proseDiv?.innerHTML).toBe('');
    });

    it('should preserve class attribute on allowed tags', () => {
        const htmlWithClass = '<p class="custom-class">Styled paragraph</p>';
        const {wrapper} = createMockForm();

        const {container} = render(<CustomHTMLFieldRenderer formInput={{defaultValue: htmlWithClass}} />, {wrapper});

        const paragraph = container.querySelector('p.custom-class');
        expect(paragraph).toBeInTheDocument();
        expect(paragraph?.textContent).toBe('Styled paragraph');
    });

    it('should render multiple heading levels', () => {
        const headingsHtml = '<h1>H1</h1><h2>H2</h2><h3>H3</h3><h4>H4</h4><h5>H5</h5><h6>H6</h6>';
        const {wrapper} = createMockForm();

        render(<CustomHTMLFieldRenderer formInput={{defaultValue: headingsHtml}} />, {wrapper});

        expect(screen.getByRole('heading', {level: 1})).toHaveTextContent('H1');
        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('H2');
        expect(screen.getByRole('heading', {level: 3})).toHaveTextContent('H3');
        expect(screen.getByRole('heading', {level: 4})).toHaveTextContent('H4');
        expect(screen.getByRole('heading', {level: 5})).toHaveTextContent('H5');
        expect(screen.getByRole('heading', {level: 6})).toHaveTextContent('H6');
    });

    it('should render with both description and HTML content', () => {
        const {wrapper} = createMockForm();

        render(
            <CustomHTMLFieldRenderer
                formInput={{
                    defaultValue: '<p>HTML content</p>',
                    fieldDescription: 'Description text',
                }}
            />,
            {wrapper}
        );

        expect(screen.getByText('Description text')).toBeInTheDocument();
        expect(screen.getByText('HTML content')).toBeInTheDocument();
    });
});
