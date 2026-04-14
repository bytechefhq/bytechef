'use client';

import * as ContextMenuPrimitive from '@radix-ui/react-context-menu';
import {Check, ChevronRight, Circle} from 'lucide-react';
import * as React from 'react';

import {twMerge} from 'tailwind-merge';

const ContextMenu = ContextMenuPrimitive.Root;
const ContextMenuTrigger = ContextMenuPrimitive.Trigger;
const ContextMenuGroup = ContextMenuPrimitive.Group;
const ContextMenuPortal = ContextMenuPrimitive.Portal;
const ContextMenuSub = ContextMenuPrimitive.Sub;
const ContextMenuRadioGroup = ContextMenuPrimitive.RadioGroup;

const ContextMenuSubTrigger = React.forwardRef<
    React.ElementRef<typeof ContextMenuPrimitive.SubTrigger>,
    React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.SubTrigger> & {
        inset?: boolean;
    }
>(({children, className, inset, ...props}, ref) => (
    <ContextMenuPrimitive.SubTrigger
        className={twMerge(
            'dropdown-menu-item flex select-none items-center text-sm outline-none data-[state=open]:bg-surface-neutral-primary-hover',
            inset && 'pl-8',
            className
        )}
        ref={ref}
        {...props}
    >
        {children}
        <ChevronRight className="ml-auto size-4" />
    </ContextMenuPrimitive.SubTrigger>
));
ContextMenuSubTrigger.displayName = ContextMenuPrimitive.SubTrigger.displayName;

const ContextMenuSubContent = React.forwardRef<
    React.ElementRef<typeof ContextMenuPrimitive.SubContent>,
    React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.SubContent>
>(({className, ...props}, ref) => (
    <ContextMenuPrimitive.SubContent
        className={twMerge(
            'z-50 min-w-[8rem] overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-md data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
            className
        )}
        ref={ref}
        {...props}
    />
));
ContextMenuSubContent.displayName = ContextMenuPrimitive.SubContent.displayName;

type ContextMenuContentProps = React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.Content> & {
    portal?: boolean;
};

const ContextMenuContent = React.forwardRef<React.ElementRef<typeof ContextMenuPrimitive.Content>, ContextMenuContentProps>(
    ({className, portal = true, ...props}, ref) => {
        const content = (
            <ContextMenuPrimitive.Content
                className={twMerge(
                    'z-[9999] min-w-[8rem] overflow-hidden rounded-md border bg-popover text-popover-foreground shadow-md',
                    'data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95',
                    className
                )}
                ref={ref}
                {...props}
            />
        );

        if (!portal) {
            return content;
        }

        return <ContextMenuPrimitive.Portal>{content}</ContextMenuPrimitive.Portal>;
    }
);
ContextMenuContent.displayName = ContextMenuPrimitive.Content.displayName;

const ContextMenuItem = React.forwardRef<
    React.ElementRef<typeof ContextMenuPrimitive.Item>,
    React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.Item> & {
        inset?: boolean;
        destructive?: boolean;
    }
>(({className, inset, destructive, ...props}, ref) => (
    <ContextMenuPrimitive.Item
        className={twMerge(
            'relative flex select-none items-center gap-2 text-sm outline-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&>svg]:size-4 [&>svg]:shrink-0',
            destructive ? 'dropdown-menu-item-destructive' : 'dropdown-menu-item',
            inset && 'pl-8',
            className
        )}
        ref={ref}
        {...props}
    />
));
ContextMenuItem.displayName = ContextMenuPrimitive.Item.displayName;

const ContextMenuCheckboxItem = React.forwardRef<
    React.ElementRef<typeof ContextMenuPrimitive.CheckboxItem>,
    React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.CheckboxItem>
>(({checked, children, className, ...props}, ref) => (
    <ContextMenuPrimitive.CheckboxItem
        checked={checked}
        className={twMerge(
            'dropdown-menu-item relative flex select-none items-center text-sm outline-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 pl-8',
            className
        )}
        ref={ref}
        {...props}
    >
        <span className="absolute left-2 flex size-3.5 items-center justify-center">
            <ContextMenuPrimitive.ItemIndicator>
                <Check className="size-4" />
            </ContextMenuPrimitive.ItemIndicator>
        </span>
        {children}
    </ContextMenuPrimitive.CheckboxItem>
));
ContextMenuCheckboxItem.displayName = ContextMenuPrimitive.CheckboxItem.displayName;

const ContextMenuRadioItem = React.forwardRef<
    React.ElementRef<typeof ContextMenuPrimitive.RadioItem>,
    React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.RadioItem>
>(({children, className, ...props}, ref) => (
    <ContextMenuPrimitive.RadioItem
        className={twMerge(
            'dropdown-menu-item relative flex select-none items-center text-sm outline-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 pl-8',
            className
        )}
        ref={ref}
        {...props}
    >
        <span className="absolute left-2 flex size-3.5 items-center justify-center">
            <ContextMenuPrimitive.ItemIndicator>
                <Circle className="size-2 fill-current" />
            </ContextMenuPrimitive.ItemIndicator>
        </span>
        {children}
    </ContextMenuPrimitive.RadioItem>
));
ContextMenuRadioItem.displayName = ContextMenuPrimitive.RadioItem.displayName;

const ContextMenuLabel = React.forwardRef<
    React.ElementRef<typeof ContextMenuPrimitive.Label>,
    React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.Label> & {
        inset?: boolean;
    }
>(({className, inset, ...props}, ref) => (
    <ContextMenuPrimitive.Label
        className={twMerge('px-4 py-2 text-sm font-semibold text-foreground', inset && 'pl-8', className)}
        ref={ref}
        {...props}
    />
));
ContextMenuLabel.displayName = ContextMenuPrimitive.Label.displayName;

const ContextMenuSeparator = React.forwardRef<
    React.ElementRef<typeof ContextMenuPrimitive.Separator>,
    React.ComponentPropsWithoutRef<typeof ContextMenuPrimitive.Separator>
>(({className, ...props}, ref) => (
    <ContextMenuPrimitive.Separator className={twMerge('h-px bg-border', className)} ref={ref} {...props} />
));
ContextMenuSeparator.displayName = ContextMenuPrimitive.Separator.displayName;

const ContextMenuShortcut = ({className, ...props}: React.HTMLAttributes<HTMLSpanElement>) => {
    return <span className={twMerge('ml-auto text-xs tracking-widest text-muted-foreground', className)} {...props} />;
};
ContextMenuShortcut.displayName = 'ContextMenuShortcut';

export {
    ContextMenu,
    ContextMenuCheckboxItem,
    ContextMenuContent,
    ContextMenuGroup,
    ContextMenuItem,
    ContextMenuLabel,
    ContextMenuPortal,
    ContextMenuRadioGroup,
    ContextMenuRadioItem,
    ContextMenuSeparator,
    ContextMenuShortcut,
    ContextMenuSub,
    ContextMenuSubContent,
    ContextMenuSubTrigger,
    ContextMenuTrigger,
};