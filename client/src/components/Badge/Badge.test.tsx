import {render, screen} from '@/shared/util/test-utils';
import {CheckIcon} from 'lucide-react';
import {describe, expect, it} from 'vitest';

import Badge from './Badge';

it('should render a default badge with text if no styling props are set', () => {
    render(<Badge label="Badge" />);

    expect(screen.getByText('Badge')).toHaveTextContent('Badge');
    expect(screen.getByText('Badge')).toHaveClass('px-2 py-0.5');
    expect(screen.getByText('Badge')).toHaveClass('font-normal');
    expect(screen.getByText('Badge')).toHaveClass('bg-surface-brand-primary text-content-onsurface-primary');
});

it('should render a badge with text and icon', () => {
    const {container} = render(<Badge icon={<CheckIcon />} label="Badge" />);

    expect(screen.getByText('Badge')).toHaveTextContent('Badge');
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-check');
});

describe('Badge variants', () => {
    it('should render a primary filled badge if styleType is set to primary-filled', () => {
        render(<Badge label="Badge" styleType="primary-filled" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-brand-primary text-content-onsurface-primary');
    });

    it('should render a primary outline badge if styleType is set to primary-outline', () => {
        render(<Badge label="Badge" styleType="primary-outline" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-brand-secondary text-content-brand-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-brand-primary');
    });

    it('should render a secondary filled badge if styleType is set to secondary-filled', () => {
        render(<Badge label="Badge" styleType="secondary-filled" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-neutral-secondary text-content-neutral-primary');
    });

    it('should render a secondary outline badge if styleType is set to secondary-outline', () => {
        render(<Badge label="Badge" styleType="secondary-outline" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-neutral-secondary text-content-neutral-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-neutral-secondary');
    });

    it('should render an outline badge if styleType is set to outline-outline', () => {
        render(<Badge label="Badge" styleType="outline-outline" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-neutral-primary text-content-neutral-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-neutral-secondary');
    });

    it('should render a success filled badge if styleType is set to success-filled', () => {
        render(<Badge label="Badge" styleType="success-filled" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-success-primary text-content-onsurface-primary');
    });

    it('should render a success outline badge if styleType is set to success-outline', () => {
        render(<Badge label="Badge" styleType="success-outline" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-success-secondary text-content-success-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-success-primary');
    });

    it('should render a warning filled badge if styleType is set to warning-filled', () => {
        render(<Badge label="Badge" styleType="warning-filled" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-warning-secondary text-content-warning-primary');
    });

    it('should render a warning outline badge if styleType is set to warning-outline', () => {
        render(<Badge label="Badge" styleType="warning-outline" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-warning-secondary text-content-warning-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-warning-primary');
    });

    it('should render a destructive filled badge if styleType is set to destructive-filled', () => {
        render(<Badge label="Badge" styleType="destructive-filled" />);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-destructive-primary text-content-onsurface-primary');
    });

    it('should render a destructive outline badge if styleType is set to destructive-outline', () => {
        render(<Badge label="Badge" styleType="destructive-outline" />);

        expect(screen.getByText('Badge')).toHaveClass(
            'bg-surface-destructive-secondary text-content-destructive-primary'
        );
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-destructive-primary');
    });
});

describe('Badge weights', () => {
    it('should render a regular weight badge if weight is set to regular', () => {
        render(<Badge label="Badge" weight="regular" />);

        expect(screen.getByText('Badge')).toHaveClass('font-normal');
    });

    it('should render a semibold weight badge if weight is set to semibold', () => {
        render(<Badge label="Badge" weight="semibold" />);

        expect(screen.getByText('Badge')).toHaveClass('font-semibold');
    });
});

describe('TypeScript tests', () => {
    describe('aria-label requirements', () => {
        it('should require aria-label for icon-only badges', () => {
            // @ts-expect-error - aria-label is required for icon-only badges
            render(<Badge icon={<CheckIcon />} />);
        });

        it('should apply aria-label correctly when provided for icon-only badges', () => {
            render(<Badge aria-label="Success indicator" icon={<CheckIcon />} />);

            const badge = screen.getByLabelText('Success indicator');
            expect(badge).toBeInTheDocument();
            expect(badge).toHaveAttribute('aria-label', 'Success indicator');
        });

        it('should not allow aria-label for text-only badges', () => {
            // @ts-expect-error - aria-label is not allowed for text-only badges
            render(<Badge aria-label="Some label" label="Badge" />);
        });

        it('should not allow aria-label for icon+text badges', () => {
            render(
                // @ts-expect-error - aria-label is not allowed for icon+text badges (text serves as accessible name)
                <Badge aria-label="Some label" icon={<CheckIcon />} label="Badge" />
            );
        });
    });

    describe('className prop', () => {
        it('should allow className prop to be set', () => {
            render(<Badge className="ring-1" label="Badge" />);

            expect(screen.getByText('Badge')).toHaveClass('ring-1');
        });

        it('should merge className with default styles using twMerge', () => {
            render(<Badge className="my-2 rounded-full" label="Badge" />);

            const badge = screen.getByText('Badge');
            expect(badge).toHaveClass('rounded-full');
            expect(badge).toHaveClass('my-2');
            expect(badge).toHaveClass('px-2');
        });
    });

    describe('prop type safety', () => {
        it('should require label or children for badges', () => {
            // @ts-expect-error - label or children is required for badges
            render(<Badge />);
        });

        it('should allow valid styleType values', () => {
            const validStyleTypes = [
                'primary-filled',
                'primary-outline',
                'secondary-filled',
                'secondary-outline',
                'outline-outline',
                'success-filled',
                'success-outline',
                'warning-filled',
                'warning-outline',
                'destructive-filled',
                'destructive-outline',
            ] as const;

            validStyleTypes.forEach((styleType) => {
                render(<Badge label="Badge" styleType={styleType} />);
            });
        });

        it('should not allow invalid styleType values', () => {
            // @ts-expect-error - invalid styleType value
            render(<Badge label="Badge" styleType="invalid-style" />);
        });

        it('should allow valid weight values', () => {
            render(<Badge label="Badge" weight="regular" />);
            render(<Badge label="Badge" weight="semibold" />);
        });

        it('should not allow invalid weight values', () => {
            // @ts-expect-error - invalid weight value
            render(<Badge label="Badge" weight="bold" />);
        });
    });
});

describe('Badge with all props combined', () => {
    it('should render a badge with all props combined', () => {
        const {container} = render(
            <Badge
                className="ring-1"
                icon={<CheckIcon />}
                label="Badge"
                styleType="success-outline"
                weight="semibold"
            />
        );

        expect(screen.getByText('Badge')).toBeInTheDocument();
        expect(container.querySelector('svg')).toBeInTheDocument();
        expect(screen.getByText('Badge')).toHaveClass('font-semibold');
        expect(screen.getByText('Badge')).toHaveClass('bg-surface-success-secondary');
        expect(screen.getByText('Badge')).toHaveClass('text-content-success-primary');
        expect(screen.getByText('Badge')).toHaveClass('ring-1');
    });

    it('should render an icon badge with all props combined', () => {
        const {container} = render(
            <Badge
                aria-label="Success"
                className="ring-1"
                icon={<CheckIcon />}
                styleType="warning-filled"
                weight="semibold"
            />
        );

        expect(container.querySelector('svg')).toBeInTheDocument();
        expect(container.querySelector('svg')).toHaveClass('lucide-check');

        const badgeDiv = container.querySelector('div');
        expect(badgeDiv).toHaveClass('p-0.5');
        expect(badgeDiv).toHaveClass('font-semibold');
        expect(badgeDiv).toHaveClass('bg-surface-warning-secondary');
        expect(badgeDiv).toHaveClass('text-content-warning-primary');
        expect(badgeDiv).toHaveClass('ring-1');
    });
});

describe('Badge with custom children', () => {
    it('should render a badge with custom children', () => {
        render(
            <Badge styleType="primary-filled">
                <span className="font-semibold">Custom</span>

                <span>Content</span>
            </Badge>
        );

        expect(screen.getByText('Custom')).toBeInTheDocument();
        expect(screen.getByText('Content')).toBeInTheDocument();
    });

    it('should render a badge with custom children and icon', () => {
        const {container} = render(
            <Badge icon={<CheckIcon />} styleType="success-filled">
                <span>Active</span>
            </Badge>
        );

        expect(screen.getByText('Active')).toBeInTheDocument();
        expect(container.querySelector('svg')).toBeInTheDocument();
    });

    it('should not allow label and children together', () => {
        render(
            // @ts-expect-error - label and children cannot be used together
            <Badge label="Badge">
                <span>Custom</span>
            </Badge>
        );
    });
});
