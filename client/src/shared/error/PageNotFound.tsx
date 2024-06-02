import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import React from 'react';

const PageNotFound = () => {
    return (
        <PublicLayoutContainer className="my-32">
            <div className="mx-auto max-w-screen-md">
                <h1 className="text-lg">Oops!</h1>

                <p>The page does not exist.</p>
            </div>
        </PublicLayoutContainer>
    );
};

export default PageNotFound;
