import {
    DropdownMenu as ShadcnDropdownMenu,
    DropdownMenuCheckboxItem as ShadcnDropdownMenuCheckboxItem,
    DropdownMenuContent as ShadcnDropdownMenuContent,
    DropdownMenuGroup as ShadcnDropdownMenuGroup,
    DropdownMenuItem as ShadcnDropdownMenuItem,
    DropdownMenuLabel as ShadcnDropdownMenuLabel,
    DropdownMenuPortal as ShadcnDropdownMenuPortal,
    DropdownMenuRadioGroup as ShadcnDropdownMenuRadioGroup,
    DropdownMenuRadioItem as ShadcnDropdownMenuRadioItem,
    DropdownMenuSeparator as ShadcnDropdownMenuSeparator,
    DropdownMenuShortcut as ShadcnDropdownMenuShortcut,
    DropdownMenuSub as ShadcnDropdownMenuSub,
    DropdownMenuSubContent as ShadcnDropdownMenuSubContent,
    DropdownMenuSubTrigger as ShadcnDropdownMenuSubTrigger,
    DropdownMenuTrigger as ShadcnDropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {type ComponentPropsWithRef, type ReactElement, type ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface LabelContentI {
    asChild?: never;
    children?: never;
    icon?: ReactElement;
    label: string;
}

interface CustomContentI {
    children: ReactNode;
    icon?: never;
    label?: never;
}

type MenuItemContentType = LabelContentI | CustomContentI;

type DropdownMenuItemVariantType = 'default' | 'destructive';

type DropdownMenuContentPropsType = ComponentPropsWithRef<typeof ShadcnDropdownMenuContent>;

interface BaseDropdownMenuItemProps extends Omit<
    ComponentPropsWithRef<typeof ShadcnDropdownMenuItem>,
    'children' | 'inset' | 'variant'
> {
    variant?: DropdownMenuItemVariantType;
}

type DropdownMenuItemPropsType = BaseDropdownMenuItemProps & MenuItemContentType;

type DropdownMenuCheckboxItemPropsType = Omit<
    ComponentPropsWithRef<typeof ShadcnDropdownMenuCheckboxItem>,
    'asChild' | 'children'
> &
    MenuItemContentType;

type DropdownMenuLabelPropsType = Omit<ComponentPropsWithRef<typeof ShadcnDropdownMenuLabel>, 'inset'>;

type DropdownMenuRadioItemPropsType = Omit<
    ComponentPropsWithRef<typeof ShadcnDropdownMenuRadioItem>,
    'asChild' | 'children'
> &
    MenuItemContentType;

type DropdownMenuSeparatorPropsType = ComponentPropsWithRef<typeof ShadcnDropdownMenuSeparator>;

type DropdownMenuShortcutPropsType = ComponentPropsWithRef<typeof ShadcnDropdownMenuShortcut>;

type DropdownMenuSubContentPropsType = ComponentPropsWithRef<typeof ShadcnDropdownMenuSubContent>;

type DropdownMenuSubTriggerPropsType = Omit<
    ComponentPropsWithRef<typeof ShadcnDropdownMenuSubTrigger>,
    'asChild' | 'children' | 'inset'
> &
    MenuItemContentType;

const contentStyles =
    'rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-primary p-1 text-content-neutral-primary shadow-md';

const itemStyles = 'cursor-pointer gap-2 rounded-md py-1.5 text-sm font-light';

const itemVariants: Record<DropdownMenuItemVariantType, string> = {
    default:
        "text-content-neutral-primary focus:bg-surface-neutral-primary-hover focus:text-content-neutral-primary [&_svg:not([class*='text-'])]:text-content-neutral-primary",
    destructive:
        "text-content-destructive-primary focus:bg-surface-destructive-secondary focus:text-content-destructive-primary [&_svg:not([class*='text-'])]:text-content-destructive-primary",
};

const labelStyles = 'text-content-neutral-primary';

const separatorStyles = 'bg-stroke-neutral-secondary';

const shortcutStyles = 'text-content-neutral-secondary';

const subTriggerStyles =
    'data-[state=open]:bg-surface-neutral-primary-hover data-[state=open]:text-content-neutral-primary';

function DropdownMenuContent({className, ...props}: DropdownMenuContentPropsType) {
    return <ShadcnDropdownMenuContent className={twMerge(contentStyles, className)} {...props} />;
}

DropdownMenuContent.displayName = 'DropdownMenuContent';

function DropdownMenuItem({
    children,
    className,
    icon,
    label,
    variant = 'default',
    ...props
}: DropdownMenuItemPropsType) {
    return (
        <ShadcnDropdownMenuItem className={twMerge(itemStyles, itemVariants[variant], className)} {...props}>
            {label === undefined ? (
                children
            ) : (
                <>
                    {icon}

                    {label}
                </>
            )}
        </ShadcnDropdownMenuItem>
    );
}

DropdownMenuItem.displayName = 'DropdownMenuItem';

function DropdownMenuCheckboxItem({children, className, icon, label, ...props}: DropdownMenuCheckboxItemPropsType) {
    return (
        <ShadcnDropdownMenuCheckboxItem className={twMerge(itemStyles, itemVariants.default, className)} {...props}>
            {icon}

            {label ?? children}
        </ShadcnDropdownMenuCheckboxItem>
    );
}

DropdownMenuCheckboxItem.displayName = 'DropdownMenuCheckboxItem';

function DropdownMenuLabel({className, ...props}: DropdownMenuLabelPropsType) {
    return <ShadcnDropdownMenuLabel className={twMerge(labelStyles, className)} {...props} />;
}

DropdownMenuLabel.displayName = 'DropdownMenuLabel';

function DropdownMenuRadioItem({children, className, icon, label, ...props}: DropdownMenuRadioItemPropsType) {
    return (
        <ShadcnDropdownMenuRadioItem className={twMerge(itemStyles, itemVariants.default, className)} {...props}>
            {icon}

            {label ?? children}
        </ShadcnDropdownMenuRadioItem>
    );
}

DropdownMenuRadioItem.displayName = 'DropdownMenuRadioItem';

function DropdownMenuSeparator({className, ...props}: DropdownMenuSeparatorPropsType) {
    return <ShadcnDropdownMenuSeparator className={twMerge(separatorStyles, className)} {...props} />;
}

DropdownMenuSeparator.displayName = 'DropdownMenuSeparator';

function DropdownMenuShortcut({className, ...props}: DropdownMenuShortcutPropsType) {
    return <ShadcnDropdownMenuShortcut className={twMerge(shortcutStyles, className)} {...props} />;
}

DropdownMenuShortcut.displayName = 'DropdownMenuShortcut';

function DropdownMenuSubContent({className, ...props}: DropdownMenuSubContentPropsType) {
    return <ShadcnDropdownMenuSubContent className={twMerge(contentStyles, className)} {...props} />;
}

DropdownMenuSubContent.displayName = 'DropdownMenuSubContent';

function DropdownMenuSubTrigger({children, className, icon, label, ...props}: DropdownMenuSubTriggerPropsType) {
    return (
        <ShadcnDropdownMenuSubTrigger
            className={twMerge(itemStyles, itemVariants.default, subTriggerStyles, className)}
            {...props}
        >
            {icon}

            {label ?? children}
        </ShadcnDropdownMenuSubTrigger>
    );
}

DropdownMenuSubTrigger.displayName = 'DropdownMenuSubTrigger';

const DropdownMenu = ShadcnDropdownMenu;
const DropdownMenuGroup = ShadcnDropdownMenuGroup;
const DropdownMenuPortal = ShadcnDropdownMenuPortal;
const DropdownMenuRadioGroup = ShadcnDropdownMenuRadioGroup;
const DropdownMenuSub = ShadcnDropdownMenuSub;
const DropdownMenuTrigger = ShadcnDropdownMenuTrigger;

export {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuPortal,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuShortcut,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
};

export type {
    DropdownMenuCheckboxItemPropsType as DropdownMenuCheckboxItemProps,
    DropdownMenuContentPropsType as DropdownMenuContentProps,
    DropdownMenuItemPropsType as DropdownMenuItemProps,
    DropdownMenuItemVariantType,
    DropdownMenuRadioItemPropsType as DropdownMenuRadioItemProps,
    DropdownMenuSubTriggerPropsType as DropdownMenuSubTriggerProps,
};
