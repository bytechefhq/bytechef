import {Indicator, Root} from '@radix-ui/react-checkbox';
import {CheckIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {Label} from '@radix-ui/react-label';
import Tooltip from 'components/Tooltip/Tooltip';
import {twMerge} from 'tailwind-merge';

export interface CheckboxProps {
    defaultChecked?: boolean;
    description?: string;
    disabled?: boolean;
    id: string;
    label?: string;
}

const Checkbox = ({
    defaultChecked = true,
    description,
    disabled = false,
    id,
    label,
}: CheckboxProps) => (
    <fieldset className="flex w-full items-center space-x-1 py-2">
        {label && (
            <Label
                className={twMerge(
                    'mr-2 flex items-center text-sm font-medium leading-6 text-gray-900',
                    description && 'space-x-1'
                )}
                htmlFor={id}
            >
                <span>{label}</span>

                {description && (
                    <Tooltip text={description}>
                        <QuestionMarkCircledIcon />
                    </Tooltip>
                )}
            </Label>
        )}

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
    </fieldset>
);

export default Checkbox;
