import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import React from 'react';

const PageNotFound = () => {
    return (
        <PublicLayoutContainer>
            <div className="flex flex-col items-center gap-2">
                <h1 className="text-lg font-semibold">Oops!</h1>

                <p>The page does not exist.</p>
            </div>
        </PublicLayoutContainer>
    );
};

export default PageNotFound;
