import {render, screen} from '@/shared/util/test-utils';
import {CheckIcon} from 'lucide-react';
import {describe, expect, it} from 'vitest';

import Badge from './Badge';

it('should render a default badge with text if no styling props are set', () => {
    render(<Badge>Badge</Badge>);

    expect(screen.getByText('Badge')).toHaveTextContent('Badge');
    expect(screen.getByText('Badge')).toHaveClass('px-2 py-0.5');
    expect(screen.getByText('Badge')).toHaveClass('font-normal');
    expect(screen.getByText('Badge')).toHaveClass('bg-surface-brand-primary text-content-onsurface-primary');
});

it('should render a badge with text and icon', () => {
    const {container} = render(<Badge icon={<CheckIcon />}>Badge</Badge>);

    expect(screen.getByText('Badge')).toHaveTextContent('Badge');
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-check');
});

describe('Badge variants', () => {
    it('should render a primary filled badge if styleType is set to primary-filled', () => {
        render(<Badge styleType="primary-filled">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-brand-primary text-content-onsurface-primary');
    });

    it('should render a primary outline badge if styleType is set to primary-outline', () => {
        render(<Badge styleType="primary-outline">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-brand-secondary text-content-brand-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-brand-primary');
    });

    it('should render a secondary filled badge if styleType is set to secondary-filled', () => {
        render(<Badge styleType="secondary-filled">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-neutral-secondary text-content-neutral-primary');
    });

    it('should render a secondary outline badge if styleType is set to secondary-outline', () => {
        render(<Badge styleType="secondary-outline">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-neutral-secondary text-content-neutral-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-neutral-secondary');
    });

    it('should render an outline badge if styleType is set to outline-outline', () => {
        render(<Badge styleType="outline-outline">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-neutral-primary text-content-neutral-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-neutral-secondary');
    });

    it('should render a success filled badge if styleType is set to success-filled', () => {
        render(<Badge styleType="success-filled">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-success-primary text-content-onsurface-primary');
    });

    it('should render a success outline badge if styleType is set to success-outline', () => {
        render(<Badge styleType="success-outline">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-success-secondary text-content-success-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-success-primary');
    });

    it('should render a warning filled badge if styleType is set to warning-filled', () => {
        render(<Badge styleType="warning-filled">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-warning-secondary text-content-warning-primary');
    });

    it('should render a warning outline badge if styleType is set to warning-outline', () => {
        render(<Badge styleType="warning-outline">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-warning-secondary text-content-warning-primary');
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-warning-primary');
    });

    it('should render a destructive filled badge if styleType is set to destructive-filled', () => {
        render(<Badge styleType="destructive-filled">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('bg-surface-destructive-primary text-content-onsurface-primary');
    });

    it('should render a destructive outline badge if styleType is set to destructive-outline', () => {
        render(<Badge styleType="destructive-outline">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass(
            'bg-surface-destructive-secondary text-content-destructive-primary'
        );
        expect(screen.getByText('Badge')).toHaveClass('border-stroke-destructive-primary');
    });
});

describe('Badge weights', () => {
    it('should render a regular weight badge if weight is set to regular', () => {
        render(<Badge weight="regular">Badge</Badge>);

        expect(screen.getByText('Badge')).toHaveClass('font-normal');
    });

    it('should render a semibold weight badge if weight is set to semibold', () => {
        render(<Badge weight="semibold">Badge</Badge>);

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
            render(<Badge aria-label="Some label">Badge</Badge>);
        });

        it('should not allow aria-label for icon+text badges', () => {
            render(
                // @ts-expect-error - aria-label is not allowed for icon+text badges (text serves as accessible name)
                <Badge aria-label="Some label" icon={<CheckIcon />}>
                    Badge
                </Badge>
            );
        });
    });

    describe('className prop', () => {
        it('should allow className prop to be set', () => {
            render(<Badge className="ring-1">Badge</Badge>);

            expect(screen.getByText('Badge')).toHaveClass('ring-1');
        });

        it('should merge className with default styles using twMerge', () => {
            render(<Badge className="my-2 rounded-full">Badge</Badge>);

            const badge = screen.getByText('Badge');
            expect(badge).toHaveClass('rounded-full');
            expect(badge).toHaveClass('my-2');
            expect(badge).toHaveClass('px-2');
        });
    });

    describe('prop type safety', () => {
        it('should require children for text-only badges', () => {
            // @ts-expect-error - children is required for text-only badges
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
                render(<Badge styleType={styleType}>Badge</Badge>);
            });
        });

        it('should not allow invalid styleType values', () => {
            // @ts-expect-error - invalid styleType value
            render(<Badge styleType="invalid-style">Badge</Badge>);
        });

        it('should allow valid weight values', () => {
            render(<Badge weight="regular">Badge</Badge>);
            render(<Badge weight="semibold">Badge</Badge>);
        });

        it('should not allow invalid weight values', () => {
            // @ts-expect-error - invalid weight value
            render(<Badge weight="bold">Badge</Badge>);
        });
    });
});

describe('Badge with all props combined', () => {
    it('should render a badge with all props combined', () => {
        const {container} = render(
            <Badge className="ring-1" icon={<CheckIcon />} styleType="success-outline" weight="semibold">
                Badge
            </Badge>
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
