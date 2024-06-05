import reactLogo from '@/assets/logo.svg';
import {Toaster} from '@/components/ui/toaster';
import React, {PropsWithChildren} from 'react';

const PublicLayoutContainer = ({children}: PropsWithChildren) => {
    return (
        <>
            <div className="grid size-full place-items-center">
                <div className="w-full">
                    <div className="mb-8 flex items-center justify-center space-x-2">
                        <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />

                        <span className="text-xl font-semibold">ByteChef</span>
                    </div>

                    {children}
                </div>
            </div>

            <Toaster />
        </>
    );
};

export default PublicLayoutContainer;
