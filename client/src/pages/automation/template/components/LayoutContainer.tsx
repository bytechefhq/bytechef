import reactLogo from '@/assets/logo.svg';
import {Card} from '@/components/ui/card';
import {ReactNode} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const LayoutContainer = ({children, fromInternalFlow = false}: {children: ReactNode; fromInternalFlow?: boolean}) => {
    return (
        <div className={twMerge('flex size-full flex-col', !fromInternalFlow && 'bg-surface-main')}>
            {!fromInternalFlow && (
                <div className="absolute left-9 top-6 flex items-center justify-center py-4">
                    <Link to="/">
                        <img alt="ByteChef" className="h-8 w-auto cursor-pointer" src={reactLogo} />
                    </Link>
                </div>
            )}

            <div className="flex flex-1 items-center justify-center px-6 py-8">
                <Card className="w-8/12 overflow-hidden">
                    <div className="flex h-[600px]">{children}</div>
                </Card>
            </div>
        </div>
    );
};

export default LayoutContainer;
