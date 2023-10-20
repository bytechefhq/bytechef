import {CheckIcon} from '@radix-ui/react-icons';
import React from 'react';
import {Indicator, Root} from '@radix-ui/react-checkbox';

export interface CheckboxProps {
    defaultChecked?: boolean;
    disabled?: boolean;
    id: string;
}

const Checkbox = ({
    defaultChecked = true,
    disabled = false,
    id,
}: CheckboxProps) => (
    <Root
        id={id}
        defaultChecked={defaultChecked}
        disabled={disabled}
        className="flex h-5 w-5 items-center justify-center rounded focus:outline-none focus-visible:ring focus-visible:ring-blue-500/75 radix-state-checked:bg-blue-600 radix-state-unchecked:bg-gray-100 dark:radix-state-unchecked:bg-gray-900"
    >
        <Indicator>
            <CheckIcon className="h-4 w-4 self-center text-white" />
        </Indicator>
    </Root>
);

export default Checkbox;
