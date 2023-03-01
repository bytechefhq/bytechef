import {
    Content,
    Item,
    Portal,
    Root,
    Separator,
    Trigger,
} from '@radix-ui/react-dropdown-menu';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {ReactNode} from 'react';

export interface DropDownMenuItem {
    icon?: ReactNode;
    integrationId?: number;
    label?: string;
    onClick?: (id: number, event: React.MouseEvent) => void;
    separator?: boolean;
    shortcut?: string;
}

export const Dropdown: React.FC<{
    menuItems: DropDownMenuItem[];
    id?: number;
}> = ({id = 0, menuItems}) => {
    return (
        <div>
            <Root>
                <Trigger asChild>
                    <div className="flex h-8 w-7 cursor-pointer items-center justify-center rounded hover:bg-gray-100">
                        <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer dark:text-white" />
                    </div>
                </Trigger>

                <Portal>
                    <Content
                        align="end"
                        // eslint-disable-next-line tailwindcss/no-custom-classname
                        className="w-48 rounded-lg border border-gray-50 bg-white p-1.5 shadow-md radix-side-bottom:animate-slide-down radix-side-top:animate-slide-up dark:bg-gray-800 md:w-56"
                        id={id.toString()}
                        sideOffset={5}
                    >
                        {menuItems.map((menuItem, i) => {
                            const {label, onClick, separator} = menuItem;

                            return (
                                <div
                                    key={`menu-item-${
                                        label || 'separator'
                                    }-${i}`}
                                >
                                    {separator ? (
                                        <Separator className="my-1 h-px bg-gray-200 dark:bg-gray-700" />
                                    ) : (
                                        <Item
                                            className="flex cursor-default select-none items-center rounded-md px-4 py-2 text-xs text-gray-400 outline-none hover:cursor-pointer focus:bg-gray-50 dark:text-gray-500 dark:focus:bg-gray-900"
                                            onClick={(event) => {
                                                if (onClick) {
                                                    onClick(id, event);
                                                }
                                            }}
                                        >
                                            <span className="grow text-gray-700 dark:text-gray-300">
                                                {label}
                                            </span>
                                        </Item>
                                    )}
                                </div>
                            );
                        })}
                    </Content>
                </Portal>
            </Root>
        </div>
    );
};
