import Button from 'components/Button/Button';
import React, {ReactNode} from 'react';

const PageHeader: React.FC<{
    title: string;
    buttonLabel?: string;
    bold?: boolean;
    buttonOnClick?: () => void;
    right?: ReactNode;
}> = ({title, right, bold = false, buttonLabel, buttonOnClick}) => (
    <div className="mb-6 flex justify-center py-4">
        <div className="flex w-full items-center justify-between">
            {bold ? (
                <h1 className="text-2xl font-semibold tracking-tight text-gray-900 dark:text-gray-200">
                    {title}
                </h1>
            ) : (
                <h2 className="text-2xl tracking-tight text-gray-900 dark:text-gray-200">
                    {title}
                </h2>
            )}

            {right && <div>{right}</div>}

            {buttonLabel && buttonOnClick && <Button label={buttonLabel} />}
        </div>
    </div>
);

export default PageHeader;
