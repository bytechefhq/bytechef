import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

export type DropdownMenuItemType = {
    danger?: boolean;
    icon?: ReactNode;
    integrationId?: number;
    label?: string;
    onClick?: (id: number, event: React.MouseEvent) => void;
    separator?: boolean;
    shortcut?: string;
};

interface PropertyDropdownMenuProps {
    menuItems: Array<DropdownMenuItemType>;
    trigger?: ReactNode;
    id?: number;
}

const PropertyDropdownMenu = ({id = 0, menuItems, trigger}: PropertyDropdownMenuProps) => (
    <DropdownMenu>
        <DropdownMenuTrigger asChild>
            {trigger ? (
                trigger
            ) : (
                <div className="flex h-8 w-7 cursor-pointer items-center justify-center rounded hover:bg-gray-100">
                    <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                </div>
            )}
        </DropdownMenuTrigger>

        <DropdownMenuContent
            align="end"
            className="data-[side=bottom]:animate-slide-down data-[side=top]:animate-slide-up z-50 rounded-lg border border-gray-50 bg-white p-1.5 shadow-md"
            id={id.toString()}
        >
            {menuItems.map((menuItem, i) => {
                const {danger, icon, label, onClick, separator} = menuItem;

                return (
                    <div key={`menu-item-${label || 'separator'}-${i}`}>
                        {separator ? (
                            <DropdownMenuSeparator className="my-1 h-px bg-gray-200" />
                        ) : (
                            <DropdownMenuItem
                                className={twMerge(
                                    'flex cursor-default select-none items-center rounded-md px-4 py-2 text-sm text-gray-700 outline-none hover:cursor-pointer hover:bg-gray-50',
                                    danger && 'text-red-600 hover:bg-red-600 hover:text-white'
                                )}
                                onClick={(event) => {
                                    if (onClick) {
                                        onClick(id, event);
                                    }
                                }}
                            >
                                {icon}

                                <span className={twMerge('grow', icon && 'ml-2')}>{label}</span>
                            </DropdownMenuItem>
                        )}
                    </div>
                );
            })}
        </DropdownMenuContent>
    </DropdownMenu>
);

export default PropertyDropdownMenu;
