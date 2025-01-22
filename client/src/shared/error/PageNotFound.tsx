import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import React from 'react';
import {Link} from 'react-router-dom';

const PageNotFound = () => {
    return (
        <PublicLayoutContainer>
            <div className="flex flex-col items-center justify-center px-4 sm:px-6 lg:px-8">
                <div className="mx-auto max-w-md text-center">
                    <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">Oops!</h1>

                    <p className="mt-4 text-muted-foreground">We are sorry, but the page does not exist.</p>

                    <div className="mt-6">
                        <Link
                            className="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                            to="/"
                        >
                            Go to Homepage
                        </Link>
                    </div>
                </div>
            </div>
        </PublicLayoutContainer>
    );
};

export default PageNotFound;
