import {buttonVariants} from '@/components/ui/button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {cn} from '@/shared/util/cn-utils';
import {ChevronRightIcon} from 'lucide-react';
import {useState} from 'react';

export interface SettingsNavGroupItemI {
    href: string;
    title: string;
}

interface SettingsNavGroupProps {
    isCurrent: (href: string) => boolean;
    items: SettingsNavGroupItemI[];
    title: string;
}

const SettingsNavGroup = ({isCurrent, items, title}: SettingsNavGroupProps) => {
    const [open, setOpen] = useState(() => items.some((item) => isCurrent(item.href)));

    const holdsCurrentItem = items.some((item) => isCurrent(item.href));

    return (
        <Collapsible onOpenChange={setOpen} open={open}>
            <CollapsibleTrigger
                className={cn(
                    buttonVariants({variant: 'ghost'}),
                    'group w-full justify-start px-2 font-normal hover:bg-accent',
                    holdsCurrentItem && !open && 'bg-accent hover:bg-accent'
                )}
            >
                <span className={cn('truncate', holdsCurrentItem && !open && 'font-semibold')}>{title}</span>

                <ChevronRightIcon className="ml-auto size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-90" />
            </CollapsibleTrigger>

            <CollapsibleContent>
                {items.map((item) => (
                    <LeftSidebarNavItem
                        className="pl-5"
                        item={{current: isCurrent(item.href), name: item.title}}
                        key={item.href}
                        toLink={item.href}
                    />
                ))}
            </CollapsibleContent>
        </Collapsible>
    );
};

export default SettingsNavGroup;
