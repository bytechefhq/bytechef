import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import ReactDatePicker, {
    ReactDatePickerCustomHeaderProps,
} from 'react-datepicker';
import {twMerge} from 'tailwind-merge';

import './DatePicker.css';

import {ChevronLeftIcon, ChevronRightIcon} from '@heroicons/react/24/solid';
import {format} from 'date-fns';
import {useState} from 'react';

type InputProps = {
    className?: string;
    name: string;
    fieldsetClassName?: string;
    error?: boolean;
    label?: string;
    labelClassName?: string;
    placeholder?: string;
    onChange: (date: Date | undefined) => void;
};

const DatePicker = ({
    className,
    fieldsetClassName,
    label,
    labelClassName,
    name,
    error,
    placeholder,
    onChange,
}: InputProps): JSX.Element => {
    const [date, setDate] = useState<Date | null>(null);

    return (
        <fieldset className={twMerge('mb-3', fieldsetClassName)}>
            {label && (
                <label
                    htmlFor={name}
                    className={twMerge(
                        'block text-sm font-medium text-gray-700 dark:text-gray-400',
                        labelClassName
                    )}
                >
                    {label}
                </label>
            )}

            <div className={twMerge(['relative', label && 'mt-1'])}>
                <ReactDatePicker
                    className={className}
                    nextMonthButtonLabel=">"
                    placeholderText={placeholder}
                    previousMonthButtonLabel="<"
                    popperClassName="react-datepicker-left"
                    popperPlacement="bottom-start"
                    renderCustomHeader={(props) => <Header {...props} />}
                    selected={date}
                    onChange={(date) => {
                        setDate(date);

                        onChange(date ? date : undefined);
                    }}
                />

                {error && (
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                        <ExclamationCircleIcon
                            className="h-5 w-5 text-rose-500"
                            aria-hidden="true"
                        />
                    </div>
                )}
            </div>

            {error && (
                <p
                    role="alert"
                    className="mt-2 text-sm text-rose-600"
                    id={`${name}-error`}
                >
                    This field is required
                </p>
            )}
        </fieldset>
    );
};

const Header = ({
    date,
    decreaseMonth,
    increaseMonth,
    prevMonthButtonDisabled,
    nextMonthButtonDisabled,
}: ReactDatePickerCustomHeaderProps): JSX.Element => (
    <div className="flex items-center justify-between p-2">
        <span className="text-lg text-gray-700">
            {format(date, 'MMMM yyyy')}
        </span>

        <div className="space-x-2">
            <button
                onClick={decreaseMonth}
                disabled={prevMonthButtonDisabled}
                type="button"
                className={`
                    ${
                        prevMonthButtonDisabled &&
                        'cursor-not-allowed opacity-50'
                    }
                    inline-flex rounded border border-gray-300 bg-white p-1 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-0
                `}
            >
                <ChevronLeftIcon className="h-5 w-5 text-gray-600" />
            </button>

            <button
                onClick={increaseMonth}
                disabled={nextMonthButtonDisabled}
                type="button"
                className={`
                    ${
                        nextMonthButtonDisabled &&
                        'cursor-not-allowed opacity-50'
                    }
                    inline-flex rounded border border-gray-300 bg-white p-1 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-0
                `}
            >
                <ChevronRightIcon className="h-5 w-5 text-gray-600" />
            </button>
        </div>
    </div>
);

export default DatePicker;
