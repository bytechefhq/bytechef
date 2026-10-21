import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {EllipsisVerticalIcon} from 'lucide-react';
import {MouseEvent, ReactElement, ReactNode} from 'react';

export type DropdownMenuItemType = {
    danger?: boolean;
    icon?: ReactElement;
    integrationId?: number;
    label?: string;
    onClick?: (id: number, event: MouseEvent) => void;
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
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </div>
            )}
        </DropdownMenuTrigger>

        <DropdownMenuContent align="end" id={id.toString()}>
            {menuItems.map((menuItem, i) => {
                const {danger, icon, label, onClick, separator} = menuItem;

                return (
                    <div key={`menu-item-${label || 'separator'}-${i}`}>
                        {separator ? (
                            <DropdownMenuSeparator />
                        ) : (
                            <DropdownMenuItem
                                icon={icon}
                                label={label ?? ''}
                                onClick={(event) => {
                                    if (onClick) {
                                        onClick(id, event);
                                    }
                                }}
                                variant={danger ? 'destructive' : 'default'}
                            />
                        )}
                    </div>
                );
            })}
        </DropdownMenuContent>
    </DropdownMenu>
);

export default PropertyDropdownMenu;
