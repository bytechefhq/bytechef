/* eslint-disable tailwindcss/no-custom-classname */

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
import Button from '../Button/Button';
import {Dispatch, Fragment, SetStateAction} from 'react';

interface ModalProps {
    children: React.ReactNode;
    confirmButtonLabel: string;
    description: string;
    form: boolean;
    isOpen: boolean;
    setIsOpen: Dispatch<SetStateAction<boolean>>;
    title: string;
    triggerLabel: string;
    onCloseClick?: () => void;
    onConfirmButtonClick: () => void;
}

const Modal = ({
    children,
    confirmButtonLabel,
    description,
    form,
    isOpen,
    setIsOpen,
    title,
    triggerLabel,
    onCloseClick,
    onConfirmButtonClick,
}: ModalProps): JSX.Element => (
    <Root open={isOpen} onOpenChange={setIsOpen}>
        <Trigger asChild>
            <Button label={triggerLabel} />
        </Trigger>

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
                        className="fixed top-[50%] left-[50%] z-50 w-[95vw] max-w-md translate-x-[-50%] translate-y-[-50%] rounded-lg bg-white p-4 focus:outline-none focus-visible:ring focus-visible:ring-blue-500/75 dark:bg-gray-800 md:w-full"
                    >
                        <Title className="text-base font-medium text-gray-900 dark:text-gray-100">
                            {title}
                        </Title>

                        <Description className="my-2 text-sm font-normal text-gray-700 dark:text-gray-400">
                            {description}
                        </Description>

                        {children}

                        {!form && (
                            <div className="mt-4 flex justify-end">
                                <Close asChild>
                                    <Button
                                        label={confirmButtonLabel}
                                        onClick={onConfirmButtonClick}
                                    />
                                </Close>
                            </div>
                        )}

                        <Close className="absolute top-3.5 right-3.5 inline-flex items-center justify-center rounded-full p-1 focus:outline-none focus-visible:ring focus-visible:ring-blue-500/75">
                            <Cross1Icon
                                className="h-4 w-4 text-gray-500 hover:text-gray-700 dark:text-gray-500 dark:hover:text-gray-400"
                                onClick={onCloseClick}
                            />
                        </Close>
                    </Content>
                </Transition.Child>
            </Transition.Root>
        </Portal>
    </Root>
);

export default Modal;
