import {
    Empty as ShadcnEmpty,
    EmptyContent as ShadcnEmptyContent,
    EmptyDescription as ShadcnEmptyDescription,
    EmptyHeader as ShadcnEmptyHeader,
    EmptyMedia as ShadcnEmptyMedia,
    EmptyTitle as ShadcnEmptyTitle,
} from '@/components/ui/empty';
import {BookOpenIcon} from 'lucide-react';
import {ComponentPropsWithRef, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface EmptyStateDocsLinkI {
    href: string;
    label: string;
}

interface EmptyStateProps extends Omit<ComponentPropsWithRef<'div'>, 'title'> {
    actions?: ReactNode;
    description?: ReactNode;
    docsLink?: EmptyStateDocsLinkI;
    icon: ReactNode;
    note?: ReactNode;
    title: string;
}

const basicStyles = 'w-full place-self-center 3xl:mx-auto 3xl:w-4/5';

const EmptyState = ({
    actions,
    children,
    className,
    description,
    docsLink,
    icon,
    note,
    title,
    ...props
}: EmptyStateProps) => {
    const hasContent = !!actions || !!children || !!note || !!docsLink;

    return (
        <ShadcnEmpty className={twMerge(basicStyles, className)} {...props}>
            <ShadcnEmptyHeader className="max-w-md">
                <ShadcnEmptyMedia
                    className="size-12 rounded-xl bg-surface-brand-secondary text-content-brand-primary"
                    variant="icon"
                >
                    {icon}
                </ShadcnEmptyMedia>

                <ShadcnEmptyTitle
                    aria-level={2}
                    className="text-lg font-semibold text-content-neutral-primary"
                    role="heading"
                >
                    {title}
                </ShadcnEmptyTitle>

                {description && (
                    <ShadcnEmptyDescription className="text-content-neutral-secondary">
                        {description}
                    </ShadcnEmptyDescription>
                )}
            </ShadcnEmptyHeader>

            {hasContent && (
                <ShadcnEmptyContent className="max-w-3xl">
                    {actions && <div className="flex flex-wrap justify-center gap-2">{actions}</div>}

                    {children}

                    {note && <p className="text-xs text-content-neutral-secondary">{note}</p>}

                    {docsLink && (
                        <a
                            className="inline-flex items-center gap-1 text-sm font-medium text-content-brand-primary hover:underline"
                            href={docsLink.href}
                            rel="noopener noreferrer"
                            target="_blank"
                        >
                            <BookOpenIcon aria-hidden="true" className="size-4" />

                            {docsLink.label}
                        </a>
                    )}
                </ShadcnEmptyContent>
            )}
        </ShadcnEmpty>
    );
};

EmptyState.displayName = 'EmptyState';

export default EmptyState;
export type {EmptyStateProps};
