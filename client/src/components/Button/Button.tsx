import React from 'react';

const Button: React.FC<{
    buttonLabel: string;
}> = ({buttonLabel}) => (
    <button
        type="button"
        className="inline-flex items-center rounded-md border border-transparent bg-black px-2 py-1 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:bg-sky-500 dark:hover:bg-sky-400"
    >
        {buttonLabel}
    </button>
);

export default Button;
