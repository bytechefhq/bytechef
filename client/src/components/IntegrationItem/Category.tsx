import {Squares2X2Icon} from '@heroicons/react/24/outline';
import React from 'react';
import {CategoryModel} from '../../data-access/integration';

export const Category: React.FC<{category: CategoryModel}> = ({category}) => {
    return (
        <div className="mr-4 flex items-center text-sm text-gray-500">
            <Squares2X2Icon
                className="mr-1 h-5 w-5 text-gray-400"
                aria-hidden="true"
            />

            {category.name}
        </div>
    );
};
