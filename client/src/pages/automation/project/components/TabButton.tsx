import Button from 'components/Button/Button';
import {twMerge} from 'tailwind-merge';

type TabButtonProps = {
    activeTab: string;
    handleClick: () => void;
    label: string;
    name: string;
};

const TabButton = ({
    activeTab,
    handleClick,
    label,
    name,
}: TabButtonProps): JSX.Element => (
    <Button
        className={twMerge(
            'flex grow justify-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-500 hover:text-gray-700',
            activeTab === name && 'bg-gray-100 text-gray-700'
        )}
        label={label}
        onClick={handleClick}
    />
);

export default TabButton;
