import * as PopoverPrimitive from '@radix-ui/react-popover';
import React, {ReactNode} from 'react';

const PopoverMenu = ({children}: {children: ReactNode}) => {
    return (
        <div className="relative inline-block text-left">
            <PopoverPrimitive.Root>
                <PopoverPrimitive.Trigger asChild>
                    {children}
                </PopoverPrimitive.Trigger>
                <PopoverPrimitive.Content
                    align="center"
                    sideOffset={4}
                    className="z-50 w-48 rounded-lg bg-white p-4 shadow-md radix-side-bottom:animate-slide-down radix-side-top:animate-slide-up dark:bg-gray-800 md:w-56"
                >
                    <PopoverPrimitive.Arrow className="fill-current text-white dark:text-gray-800" />
                    TODO
                </PopoverPrimitive.Content>
            </PopoverPrimitive.Root>
        </div>
    );
};

export default PopoverMenu;
