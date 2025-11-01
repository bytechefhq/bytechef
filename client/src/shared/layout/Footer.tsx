import {PropsWithChildren, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface FooterProps {
    centerTitle?: boolean;
    className?: string;
    position?: 'main' | 'sidebar';
    right?: ReactNode;
}

const Footer = ({centerTitle, children, className, position = 'sidebar', right}: PropsWithChildren<FooterProps>) => (
    <footer className={twMerge('p-4', centerTitle ? '3xl:mx-auto 3xl:w-4/5' : '3xl:w-4/5', className)}>
        <div className="flex w-full items-center justify-between">
            <div
                className={twMerge(
                    'flex h-footer-height w-full items-center text-lg tracking-tight',
                    position === 'sidebar' ? 'font-semibold' : ''
                )}
            >
                {children}
            </div>

            {right && <div>{right}</div>}
        </div>
    </footer>
);

export default Footer;
