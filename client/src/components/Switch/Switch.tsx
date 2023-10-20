import * as SwitchPrimitive from '@radix-ui/react-switch';
import React from 'react';

const Switch = () => {
    return (
        <SwitchPrimitive.Root className="group relative inline-flex h-[24px] w-[44px] shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus-visible:ring focus-visible:ring-blue-500/75 radix-state-checked:bg-blue-600 radix-state-unchecked:bg-gray-200 dark:radix-state-unchecked:bg-gray-800">
            <SwitchPrimitive.Thumb className="pointer-events-none inline-block h-[20px] w-[20px] rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out group-radix-state-checked:translate-x-5 group-radix-state-unchecked:translate-x-0" />
        </SwitchPrimitive.Root>
    );
};

export default Switch;
