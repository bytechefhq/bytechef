import {
    Content,
    Item,
    Portal,
    Root,
    Separator,
    Trigger,
} from '@radix-ui/react-dropdown-menu';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import './dropdown.css';
import {ReactNode} from 'react';

interface RadixMenuItem {
    label?: string;
    shortcut?: string;
    icon?: ReactNode;
    separator?: boolean;
}

const menuItems: RadixMenuItem[] = [
    {
        label: 'Edit',
    },
    {
        label: 'Enable',
    },
    {
        label: 'Duplicate',
    },
    {
        label: 'New Workflow',
    },
    {
        separator: true,
    },
    {
        label: 'Delete',
    },
];

export const Dropdown: React.FC<{
    id: string;
}> = ({id}) => {
    return (
        <div className="relative inline-block text-left">
            <Root>
                <Trigger asChild>
                    <DotsVerticalIcon className="dark:text-white" />
                </Trigger>

                <Portal>
                    <Content
                        align="end"
                        className="w-48 rounded-lg bg-white px-1.5 py-1 shadow-md radix-side-bottom:animate-slide-down radix-side-top:animate-slide-up dark:bg-gray-800 md:w-56"
                        id={id}
                        sideOffset={5}
                    >
                        {menuItems.map(({label, separator}, i) => (
                            <>
                                {!separator && (
                                    <Item
                                        key={`${label}-${i}`}
                                        className="flex cursor-default select-none items-center rounded-md px-4 py-2 text-xs text-gray-400 outline-none focus:bg-gray-50 dark:text-gray-500 dark:focus:bg-gray-900"
                                    >
                                        <span className="grow text-gray-700 dark:text-gray-300">
                                            {label}
                                        </span>
                                    </Item>
                                )}

                                {separator && (
                                    <Separator className="my-1 h-px bg-gray-200 dark:bg-gray-700" />
                                )}
                            </>
                        ))}
                    </Content>
                </Portal>
            </Root>
        </div>
    );
};
