import {PropsWithChildren, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface FooterProps {
    leftSidebar?: boolean;
    position?: 'main' | 'sidebar';
    right?: ReactNode;
}

const Footer = ({children, leftSidebar = false, position = 'sidebar', right}: PropsWithChildren<FooterProps>) => (
    <footer className={twMerge('p-4', position === 'main' ? 'flex w-full place-self-center 2xl:w-4/5' : '')}>
        <div className="flex w-full items-center justify-between">
            <div
                className={twMerge(
                    'flex h-[34px] w-full items-center text-lg tracking-tight',
                    leftSidebar && 'font-semibold'
                )}
            >
                {children}
            </div>

            {right && <div>{right}</div>}
        </div>
    </footer>
);

export default Footer;
