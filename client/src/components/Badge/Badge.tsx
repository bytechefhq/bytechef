import {twMerge} from 'tailwind-merge';

export interface BadgeProps {
    color: 'red' | 'green' | 'default';
    text: string;
}

const Badge = ({color = 'default', text}: BadgeProps): JSX.Element => {
    return (
        <span
            className={twMerge(
                'inline-flex rounded-full bg-gray-100 px-2 text-xs font-semibold leading-5 text-gray-800',
                color === 'green' && 'bg-green-100 text-green-800',
                color === 'red' && 'bg-red-100 px-2 text-red-800'
            )}
        >
            {text}
        </span>
    );
};

export default Badge;
