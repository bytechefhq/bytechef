import React from 'react';
import {CategoryModel} from '../../data-access/integration';

export const Category: React.FC<{category?: CategoryModel}> = ({category}) => {
    return <div>{category?.name}</div>;
};
