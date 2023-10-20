import Button from 'components/Button/Button';
import React from 'react';

const PageHeader: React.FC<{
    subTitle: string;
    buttonLabel: string;
}> = ({subTitle, buttonLabel}) => (
    <div className="mb-6 flex justify-center py-4">
        <div className="flex w-full items-center justify-between">
            <h2 className="text-2xl tracking-tight text-gray-900 dark:text-gray-200">
                {subTitle}
            </h2>

            <Button label={buttonLabel} />
        </div>
    </div>
);

export default PageHeader;
