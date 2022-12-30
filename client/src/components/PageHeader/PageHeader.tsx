import Button from 'components/Button/Button';
import React, {ReactNode} from 'react';

const PageHeader: React.FC<{
    title: string;
    buttonLabel?: string;
    buttonOnClick?: () => void;
    right?: ReactNode;
}> = ({title, right, buttonLabel, buttonOnClick}) => (
    <div className="mb-6 flex justify-center py-4">
        <div className="flex w-full items-center justify-between">
            <h2 className="text-xl tracking-tight text-gray-900 dark:text-gray-200">
                {title}
            </h2>

            {right && <div>{right}</div>}

            {buttonLabel && buttonOnClick && (
                <Button label={buttonLabel} onClick={buttonOnClick}></Button>
            )}
        </div>
    </div>
);

export default PageHeader;
