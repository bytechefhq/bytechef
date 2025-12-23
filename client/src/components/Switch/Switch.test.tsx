import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import Switch from './Switch';

describe('Switch - Basic Rendering', () => {
    it('should render a plain switch without label', () => {
        render(<Switch checked={false} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toBeInTheDocument();
        expect(switchElement).toHaveAttribute('aria-checked', 'false');
    });

    it('should render a checked switch', () => {
        render(<Switch checked={true} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('aria-checked', 'true');
    });

    it('should render a switch with label', () => {
        render(<Switch label="Enable notifications" />);

        expect(screen.getByText('Enable notifications')).toBeInTheDocument();
        expect(screen.getByRole('switch')).toBeInTheDocument();
    });

    it('should render a switch with label and description', () => {
        render(<Switch description="Receive alerts about important updates" label="Enable notifications" />);

        expect(screen.getByText('Enable notifications')).toBeInTheDocument();
        expect(screen.getByText('Receive alerts about important updates')).toBeInTheDocument();
    });

    it('should render wrapper when label is provided', () => {
        render(<Switch label="Feature toggle" />);

        expect(screen.getByTestId('switch-wrapper')).toBeInTheDocument();
    });

    it('should not render wrapper when no label is provided', () => {
        const {container} = render(<Switch checked={false} />);

        expect(container.querySelector('[data-testid="switch-wrapper"]')).not.toBeInTheDocument();
    });
});

describe('Switch - Variants', () => {
    it('should render default variant with correct track dimensions', () => {
        render(<Switch checked={false} variant="default" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('h-5');
        expect(switchElement).toHaveClass('w-9');
        expect(switchElement).toHaveClass('px-0.5');
        expect(switchElement).toHaveClass('rounded-full');
        expect(switchElement).toHaveClass('border-0');
    });

    it('should render default variant with correct thumb sizing via arbitrary variants', () => {
        render(<Switch checked={false} variant="default" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('[&>span]:size-4');
    });

    it('should render small variant with correct track dimensions', () => {
        render(<Switch checked={false} variant="small" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('h-[14px]');
        expect(switchElement).toHaveClass('w-[26px]');
        expect(switchElement).toHaveClass('px-[1px]');
        expect(switchElement).toHaveClass('rounded-[7px]');
        expect(switchElement).toHaveClass('border-0');
    });

    it('should render small variant with correct thumb sizing via arbitrary variants', () => {
        render(<Switch checked={false} variant="small" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('[&>span]:size-3');
    });

    it('should render box variant with correct track dimensions', () => {
        render(<Switch checked={false} variant="box" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('h-5');
        expect(switchElement).toHaveClass('w-9');
        expect(switchElement).toHaveClass('px-0.5');
        expect(switchElement).toHaveClass('rounded-full');
        expect(switchElement).toHaveClass('border-0');
    });

    it('should not show description in small variant', () => {
        render(<Switch description="This should not appear" label="Small" variant="small" />);

        expect(screen.queryByText('This should not appear')).not.toBeInTheDocument();
    });

    it('should show description in default variant', () => {
        render(<Switch description="This description should appear" label="Default" variant="default" />);

        expect(screen.getByText('This description should appear')).toBeInTheDocument();
    });

    it('should render box variant with border and padding', () => {
        render(<Switch label="Box variant" variant="box" />);

        const wrapper = screen.getByTestId('switch-wrapper');
        expect(wrapper).toHaveClass('rounded-lg');
        expect(wrapper).toHaveClass('border');
        expect(wrapper).toHaveClass('border-stroke-neutral-secondary');
        expect(wrapper).toHaveClass('w-fit');
        expect(wrapper).toHaveClass('p-3');
    });

    it('should apply checked styling to box variant', () => {
        render(<Switch checked={true} label="Box variant checked" variant="box" />);

        const wrapper = screen.getByTestId('switch-wrapper');
        expect(wrapper).toHaveClass('bg-surface-brand-secondary');
        expect(wrapper).toHaveClass('border-stroke-brand-secondary');
    });

    it('should apply correct border-radius to variants', () => {
        const {rerender} = render(<Switch checked={false} variant="default" />);
        let switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('rounded-full');

        rerender(<Switch checked={false} variant="box" />);
        switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('rounded-full');

        rerender(<Switch checked={false} variant="small" />);
        switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('rounded-[7px]');
    });
});

describe('Switch - Alignment', () => {
    it('should render with start alignment by default (switch on left)', () => {
        const {container} = render(<Switch label="Start alignment" />);

        const contentDiv = container.querySelector('[data-testid="switch-wrapper"] > div');
        expect(contentDiv).toHaveClass('flex-row-reverse');
    });

    it('should render with end alignment when specified (switch on right)', () => {
        const {container} = render(<Switch alignment="end" label="End alignment" />);

        const contentDiv = container.querySelector('[data-testid="switch-wrapper"] > div');
        expect(contentDiv).not.toHaveClass('flex-row-reverse');
    });

    it('should render box variant with start alignment', () => {
        const {container} = render(<Switch alignment="start" label="Box start" variant="box" />);

        const wrapper = container.querySelector('[data-testid="switch-wrapper"]');
        expect(wrapper).toHaveClass('flex');
        expect(wrapper).toHaveClass('items-start');
        expect(wrapper).toHaveClass('gap-2');
    });

    it('should render box variant with end alignment', () => {
        const {container} = render(<Switch alignment="end" label="Box end" variant="box" />);

        const wrapper = container.querySelector('[data-testid="switch-wrapper"]');
        expect(wrapper).toHaveClass('flex');
        expect(wrapper).toHaveClass('items-start');
        expect(wrapper).toHaveClass('gap-2');
    });
});

describe('Switch - States', () => {
    it('should apply checked background color', () => {
        render(<Switch checked={true} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('data-[state=checked]:bg-surface-brand-primary');
    });

    it('should apply unchecked background color', () => {
        render(<Switch checked={false} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('data-[state=unchecked]:bg-surface-neutral-secondary');
    });

    it('should apply thumb background color via arbitrary variant', () => {
        render(<Switch checked={false} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement.className).toMatch(/\[&>span\]:bg-surface-neutral-primary/);
    });

    it('should apply thumb shadow via arbitrary variant', () => {
        render(<Switch checked={false} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement.className).toMatch(/\[&>span\]:shadow-\[0_0_8px_rgba\(0,0,0,0\.15\)\]/);
    });

    it('should handle disabled state', () => {
        render(<Switch disabled />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toBeDisabled();
        expect(switchElement).toHaveClass('disabled:cursor-not-allowed');
        expect(switchElement).toHaveClass('disabled:opacity-50');
    });
});

describe('Switch - Accessibility', () => {
    it('should have role="switch"', () => {
        render(<Switch checked={false} />);

        expect(screen.getByRole('switch')).toBeInTheDocument();
    });

    it('should use label text as aria-label when label is string', () => {
        render(<Switch label="Enable feature" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('aria-label', 'Enable feature');
    });

    it('should use provided aria-label', () => {
        render(<Switch aria-label="Custom label" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('aria-label', 'Custom label');
    });

    it('should default to "switch" aria-label when no label provided', () => {
        render(<Switch checked={false} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('aria-label', 'switch');
    });

    it('should associate with id when provided', () => {
        render(<Switch id="my-switch" label="My Switch" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('id', 'my-switch');
    });
});

describe('Switch - Focus', () => {
    it('should have focus-visible styles', () => {
        render(<Switch checked={false} />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('focus-visible:ring-2');
        expect(switchElement).toHaveClass('focus-visible:ring-stroke-brand-focus');
        expect(switchElement).toHaveClass('focus-visible:ring-offset-0');
    });
});

describe('Switch - TypeScript Type Safety', () => {
    describe('label and alignment requirements', () => {
        it('should not allow alignment without label', () => {
            // @ts-expect-error - alignment requires label
            render(<Switch alignment="start" />);
        });

        it('should not allow description without label', () => {
            // @ts-expect-error - description requires label
            render(<Switch description="Some description" />);
        });

        it('should allow label with alignment', () => {
            render(<Switch alignment="start" label="Valid" />);

            expect(screen.getByText('Valid')).toBeInTheDocument();
        });

        it('should allow label with description', () => {
            render(<Switch description="Valid description" label="Valid" />);

            expect(screen.getByText('Valid')).toBeInTheDocument();
            expect(screen.getByText('Valid description')).toBeInTheDocument();
        });
    });

    describe('variant type safety', () => {
        it('should allow valid variant values', () => {
            render(<Switch label="Default" variant="default" />);
            render(<Switch label="Small" variant="small" />);
            render(<Switch label="Box" variant="box" />);
        });
    });

    describe('alignment type safety', () => {
        it('should allow valid alignment values', () => {
            render(<Switch alignment="start" label="Start" />);
            render(<Switch alignment="end" label="End" />);
        });

        it('should not allow invalid alignment values', () => {
            // @ts-expect-error - invalid alignment value
            render(<Switch alignment="center" label="Invalid" />);
        });
    });
});

describe('Switch - className prop', () => {
    it('should allow className prop to be set and merged with default styles', () => {
        render(<Switch checked={false} className="bg-red-500" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('bg-red-500');
        expect(switchElement).toHaveClass('rounded-full');
    });

    it('should merge className with variant-specific classes', () => {
        render(<Switch checked={false} className="bg-red-500" variant="small" />);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveClass('bg-red-500');
        expect(switchElement).toHaveClass('h-[14px]');
        expect(switchElement).toHaveClass('rounded-[7px]');
    });
});

describe('Switch - Event Handlers', () => {
    it('should call onCheckedChange when clicked', () => {
        const handleChange = vi.fn();
        render(<Switch checked={false} onCheckedChange={handleChange} />);

        const switchElement = screen.getByRole('switch');
        switchElement.click();

        expect(handleChange).toHaveBeenCalledTimes(1);
        expect(handleChange).toHaveBeenCalledWith(true);
    });

    it('should not call onCheckedChange when disabled', () => {
        const handleChange = vi.fn();
        render(<Switch checked={false} disabled onCheckedChange={handleChange} />);

        const switchElement = screen.getByRole('switch');
        switchElement.click();

        expect(handleChange).not.toHaveBeenCalled();
    });
});

describe('Switch - Combined Props', () => {
    it('should render default variant with all props combined', () => {
        const handleChange = vi.fn();
        render(
            <Switch
                alignment="end"
                checked={true}
                className="bg-red-500"
                description="Full description"
                id="full-switch"
                label="Full Switch"
                onCheckedChange={handleChange}
                variant="default"
            />
        );

        expect(screen.getByText('Full Switch')).toBeInTheDocument();
        expect(screen.getByText('Full description')).toBeInTheDocument();
        expect(screen.getByRole('switch')).toHaveAttribute('id', 'full-switch');
        expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByRole('switch')).toHaveClass('bg-red-500');
    });

    it('should render box variant with all props combined', () => {
        const handleChange = vi.fn();
        render(
            <Switch
                alignment="end"
                checked={true}
                className="bg-red-500"
                description="Box description"
                id="box-switch"
                label="Box Switch"
                onCheckedChange={handleChange}
                variant="box"
            />
        );

        expect(screen.getByText('Box Switch')).toBeInTheDocument();
        expect(screen.getByText('Box description')).toBeInTheDocument();
        expect(screen.getByRole('switch')).toHaveAttribute('id', 'box-switch');
        expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByTestId('switch-wrapper')).toHaveClass('rounded-lg');
        expect(screen.getByTestId('switch-wrapper')).toHaveClass('border');
        expect(screen.getByRole('switch')).toHaveClass('bg-red-500');
    });

    it('should render small variant with label', () => {
        render(<Switch alignment="start" checked={false} label="Small Switch" variant="small" />);

        expect(screen.getByText('Small Switch')).toBeInTheDocument();
        expect(screen.getByText('Small Switch')).toHaveClass('text-xs');
        expect(screen.getByText('Small Switch')).toHaveClass('leading-4');
        expect(screen.getByRole('switch')).toHaveClass('h-[14px]');
        expect(screen.getByRole('switch')).toHaveClass('w-[26px]');
    });
});

describe('Switch - React Node Labels', () => {
    it('should render with React node as label', () => {
        render(
            <Switch
                label={
                    <span>
                        <strong>Bold</strong> label
                    </span>
                }
            />
        );

        expect(screen.getByText('Bold')).toBeInTheDocument();
        expect(screen.getByText('label')).toBeInTheDocument();
    });

    it('should render with React node as description', () => {
        render(
            <Switch
                description={
                    <span>
                        Description with <em>emphasis</em>
                    </span>
                }
                label="Label"
            />
        );

        expect(screen.getByText('Description with')).toBeInTheDocument();
        expect(screen.getByText('emphasis')).toBeInTheDocument();
    });
});
