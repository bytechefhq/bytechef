import {Transition} from '@headlessui/react';
import {
    Close,
    Content,
    Description,
    Overlay,
    Portal,
    Root,
    Title,
    Trigger,
} from '@radix-ui/react-dialog';
import {Cross1Icon} from '@radix-ui/react-icons';
import {Dispatch, Fragment, ReactNode, SetStateAction} from 'react';
import {twMerge} from 'tailwind-merge';

import Button from '../Button/Button';

type DialogProps = {
    children: ReactNode;
    isOpen: boolean;
    onOpenChange: Dispatch<SetStateAction<boolean>>;
    customTrigger?: ReactNode;
    description?: string;
    large?: boolean;
    title?: string;
    triggerLabel?: string;
    wizard?: boolean;
};

const Dialog = ({
    children,
    customTrigger,
    description,
    isOpen,
    large,
    onOpenChange,
    title,
    triggerLabel,
    wizard = false,
}: DialogProps): JSX.Element => (
    <Root open={isOpen} onOpenChange={onOpenChange}>
        {customTrigger && <Trigger asChild>{customTrigger}</Trigger>}

        {!customTrigger && !!triggerLabel && (
            <Trigger asChild>
                <Button label={triggerLabel} />
            </Trigger>
        )}

        <Portal forceMount>
            <Transition.Root show={isOpen}>
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <Overlay
                        forceMount
                        className="fixed inset-0 z-20 bg-black/50"
                    />
                </Transition.Child>

                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0 scale-95"
                    enterTo="opacity-100 scale-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100 scale-100"
                    leaveTo="opacity-0 scale-95"
                >
                    <Content
                        forceMount
                        className={twMerge(
                            'fixed left-[50%] top-[50%] z-50 max-w-md translate-x-[-50%] translate-y-[-50%] rounded-lg bg-white focus:outline-none focus-visible:ring focus-visible:ring-blue-500/75 dark:bg-gray-800 md:w-full',
                            large && 'h-[700px] w-[800px] md:max-w-2xl',
                            !wizard && 'p-4'
                        )}
                    >
                        <Title className="text-base font-medium text-gray-900 dark:text-gray-100">
                            {title}
                        </Title>

                        {description && (
                            <Description className="my-4 text-sm font-normal text-gray-700 dark:text-gray-400">
                                {description}
                            </Description>
                        )}

                        {children}

                        <Close className="absolute right-3 top-4 inline-flex items-center justify-center rounded-full p-1 focus:outline-none focus-visible:ring focus-visible:ring-blue-500/75">
                            <Cross1Icon className="h-4 w-4 text-gray-500 hover:text-gray-700 dark:text-gray-500 dark:hover:text-gray-400" />
                        </Close>
                    </Content>
                </Transition.Child>
            </Transition.Root>
        </Portal>
    </Root>
);

export default Dialog;
