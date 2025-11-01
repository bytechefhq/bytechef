import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {CircleIcon} from 'lucide-react';
import {describe, expect, it, vi} from 'vitest';

import Button from './Button';

it('should render a default button with label if no styling props are set', () => {
    render(<Button label="Button" />);

    expect(screen.getByRole('button')).toHaveTextContent('Button');
    expect(screen.getByText('Button')).toHaveClass(
        'h-9 px-4 py-2 bg-surface-brand-primary text-content-onsurface-primary'
    );
    expect(screen.getByText('Button')).toHaveClass('hover:bg-surface-brand-primary-hover');
    expect(screen.getByText('Button')).toHaveClass('active:bg-surface-brand-primary-active');
});

it('should render a button with label and icon if no styling props are set', () => {
    const {container} = render(<Button icon={<CircleIcon />} label="Button" />);

    expect(screen.getByRole('button')).toHaveTextContent('Button');
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-circle');
});

describe('Button sizes', () => {
    it('should render a large size button with label if the size prop is set to lg', () => {
        render(<Button label="Button" size="lg" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');
        expect(screen.getByText('Button')).toHaveClass('h-10 px-8 py-2');
    });

    it('should render a small size button with label if the size prop is set to sm', () => {
        render(<Button label="Button" size="sm" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');
        expect(screen.getByText('Button')).toHaveClass('h-8 px-3 py-2 text-xs');
    });

    it('should render an extra small size button with label if the size prop is set to xs', () => {
        render(<Button label="Button" size="xs" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');
        expect(screen.getByText('Button')).toHaveClass('h-6 px-2 py-1 text-xs gap-1');
    });

    it('should render an extra extra small size button with label if the size prop is set to xxs', () => {
        render(<Button label="Button" size="xxs" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');
        expect(screen.getByText('Button')).toHaveClass('h-5 px-1.5 py-0.5 text-xs gap-1 [&_svg]:size-3');
    });

    it('should render an icon size button with icon if the size prop is set to icon and icon prop is set', () => {
        const {container} = render(<Button icon={<CircleIcon />} size="icon" />);

        expect(screen.getByRole('button')).toHaveClass('size-9 p-2.5');

        expect(container.querySelector('svg')).toBeInTheDocument();
        expect(container.querySelector('svg')).toHaveClass('lucide-circle');
    });

    it('should render an icon small size button with icon if the size prop is set to iconSm and icon prop is set', () => {
        const {container} = render(<Button icon={<CircleIcon />} size="iconSm" />);

        expect(screen.getByRole('button')).toHaveClass('size-8 p-2');

        expect(container.querySelector('svg')).toBeInTheDocument();
        expect(container.querySelector('svg')).toHaveClass('lucide-circle');
    });

    it('should render an icon extra small size button with icon if the size prop is set to iconXs and icon prop is set', () => {
        const {container} = render(<Button icon={<CircleIcon />} size="iconXs" />);

        expect(screen.getByRole('button')).toHaveClass('size-6 p-1');

        expect(container.querySelector('svg')).toBeInTheDocument();
        expect(container.querySelector('svg')).toHaveClass('lucide-circle');
    });

    it('should render an icon extra extra small size button with icon if the size prop is set to iconXxs and icon prop is set', () => {
        const {container} = render(<Button icon={<CircleIcon />} size="iconXxs" />);

        expect(screen.getByRole('button')).toHaveClass('size-5 p-1 [&_svg]:size-3');

        expect(container.querySelector('svg')).toBeInTheDocument();
        expect(container.querySelector('svg')).toHaveClass('lucide-circle');
    });
});

describe('Button variants', () => {
    it('should render a secondary variant button with label if the variant prop is set to secondary', () => {
        render(<Button label="Button" variant="secondary" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');

        expect(screen.getByText('Button')).toHaveClass('bg-surface-neutral-secondary');
        expect(screen.getByText('Button')).toHaveClass('text-content-neutral-primary');
        expect(screen.getByText('Button')).toHaveClass('hover:bg-surface-neutral-secondary-hover');
        expect(screen.getByText('Button')).toHaveClass(
            'active:bg-surface-brand-secondary active:text-content-brand-primary'
        );
    });

    it('should render a destructive variant button with label if the variant prop is set to destructive', () => {
        render(<Button label="Button" variant="destructive" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');

        expect(screen.getByText('Button')).toHaveClass('bg-surface-destructive-primary text-content-onsurface-primary');
        expect(screen.getByText('Button')).toHaveClass('hover:bg-surface-destructive-primary-hover');
        expect(screen.getByText('Button')).toHaveClass('active:bg-surface-destructive-primary-active');
    });

    it('should render a destructive ghost variant button with label if the variant prop is set to destructiveGhost', () => {
        render(<Button label="Button" variant="destructiveGhost" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');

        expect(screen.getByText('Button')).toHaveClass('bg-transparent opacity-50 text-content-destructive-primary ');
        expect(screen.getByText('Button')).toHaveClass(
            'hover:bg-surface-destructive-secondary-hover hover:opacity-100'
        );
        expect(screen.getByText('Button')).toHaveClass('active:bg-surface-destructive-secondary-active');
    });

    it('should render a destructive outline variant button with label if the variant prop is set to destructiveOutline', () => {
        render(<Button label="Button" variant="destructiveOutline" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');

        expect(screen.getByText('Button')).toHaveClass(
            'bg-transparent border border-stroke-onsurface-primary/70 text-content-onsurface-primary'
        );
        expect(screen.getByText('Button')).toHaveClass(
            'hover:border-stroke-onsurface-primary/100 hover:bg-transparent'
        );
    });

    it('should render an outline variant button with label if the variant prop is set to outline', () => {
        render(<Button label="Button" variant="outline" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');

        expect(screen.getByText('Button')).toHaveClass(
            'bg-surface-neutral-primary border border-stroke-neutral-secondary text-content-neutral-primary'
        );
        expect(screen.getByText('Button')).toHaveClass(
            'hover:bg-surface-neutral-primary-hover hover:border-stroke-neutral-secondary hover:text-content-neutral-primary'
        );
        expect(screen.getByText('Button')).toHaveClass(
            'active:bg-surface-brand-secondary active:border-stroke-brand-secondary active:text-content-brand-primary'
        );
    });

    it('should render a ghost variant button with label if the variant prop is set to ghost', () => {
        render(<Button label="Button" variant="ghost" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');

        expect(screen.getByText('Button')).toHaveClass('bg-transparent text-content-neutral-primary');
        expect(screen.getByText('Button')).toHaveClass(
            'hover:bg-surface-neutral-primary-hover hover:text-content-neutral-primary'
        );
        expect(screen.getByText('Button')).toHaveClass(
            'active:bg-surface-brand-secondary active:text-content-brand-primary'
        );
    });

    it('should render a link variant button with label if the variant prop is set to link', () => {
        render(<Button label="Button" variant="link" />);

        expect(screen.getByRole('button')).toHaveTextContent('Button');

        expect(screen.getByText('Button')).toHaveClass('bg-transparent text-content-neutral-primary');
        expect(screen.getByText('Button')).toHaveClass(
            'hover:bg-transparent active:bg-transparent hover:text-content-neutral-primary hover:underline'
        );
        expect(screen.getByText('Button')).toHaveClass(
            'active:bg-transparent active:text-content-brand-primary active:underline'
        );
    });
});

describe('TypeScript tests', () => {
    it('should render a button with custom content if the children are set and no label is set', () => {
        render(
            <Button>
                <div className="flex items-center gap-2">
                    <span className="font-bold">All</span>

                    <div className="rounded-md bg-background px-1.5 py-0.5 opacity-75">
                        <span className="text-content-brand-primary">110</span>
                    </div>
                </div>
            </Button>
        );

        expect(screen.getByRole('button')).toContainElement(screen.getByText('All'));
        expect(screen.getByRole('button')).toContainElement(screen.getByText('110'));
    });

    it('should not allow custom content when label is set', () => {
        render(
            // @ts-expect-error - label not allowed with custom content
            <Button label="Button">
                <span>Custom content</span>
            </Button>
        );

        expect(screen.getByRole('button')).toHaveTextContent('Button');
        expect(screen.getByRole('button')).not.toContainElement(screen.queryByText('Custom content'));
    });

    it('should not allow custom content with icon sizes', () => {
        render(
            // @ts-expect-error - custom content not allowed with icon sizes
            <Button size="icon">
                <span>Custom content</span>
            </Button>
        );

        expect(screen.getByRole('button')).not.toContainElement(screen.queryByText('Custom content'));
    });

    it('should not allow label with icon sizes', () => {
        const {container} = render(
            // @ts-expect-error - label not allowed with icon sizes
            <Button icon={<CircleIcon />} label="Button" size="icon" />
        );

        expect(container.querySelector('svg')).toHaveClass('lucide-circle');
        expect(screen.getByRole('button')).not.toContainElement(screen.queryByText('Button'));
    });

    it('should allow className prop to be set', () => {
        render(<Button className="bg-surface-destructive-primary" label="Button" />);

        expect(screen.getByRole('button')).toHaveClass('bg-surface-destructive-primary');
    });
});

describe('Button functionality', () => {
    it('should render a disabled button if the disabled prop is set to true and not call the onClick function', () => {
        const handleClick = vi.fn();

        render(<Button disabled label="Button" onClick={handleClick} />);

        expect(screen.getByRole('button')).toBeDisabled();

        fireEvent.click(screen.getByRole('button'));

        expect(handleClick).not.toHaveBeenCalled();
    });

    it('should call the onClick function if the button is clicked', () => {
        const handleClick = vi.fn();

        render(<Button label="Button" onClick={handleClick} />);

        fireEvent.click(screen.getByRole('button'));

        expect(handleClick).toHaveBeenCalled();
    });
});
