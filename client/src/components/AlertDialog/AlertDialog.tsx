import * as AlertDialogPrimitive from '@radix-ui/react-alert-dialog';
import React, {Dispatch, Fragment, SetStateAction} from 'react';
import Button from '../Button/Button';
import {
    Content,
    Description,
    Overlay,
    Portal,
    Root,
    Title,
} from '@radix-ui/react-alert-dialog';
import {Transition} from '@headlessui/react';

interface AlertDialogProps {
    danger: boolean;
    isOpen: boolean;
    message: string;
    title: string;
    setIsOpen: Dispatch<SetStateAction<boolean>>;
    onConfirmClick?: () => void;
}

const AlertDialog = ({
    danger,
    isOpen,
    message,
    title,
    setIsOpen,
    onConfirmClick,
}: AlertDialogProps) => {
    return (
        <Root open={isOpen} onOpenChange={setIsOpen}>
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
                            className="fixed top-[50%] left-[50%] z-50 w-[95vw] max-w-md translate-x-[-50%] translate-y-[-50%] rounded-lg bg-white p-4 focus:outline-none focus-visible:ring focus-visible:ring-purple-500/75 dark:bg-gray-800 md:w-full"
                        >
                            <Title className="text-sm font-medium text-gray-900 dark:text-gray-100">
                                {title}
                            </Title>

                            <Description className="mt-2 text-sm font-normal text-gray-700 dark:text-gray-400">
                                {message}
                            </Description>

                            <div className="mt-4 flex justify-end space-x-2">
                                <AlertDialogPrimitive.Cancel asChild={true}>
                                    <Button
                                        displayType="lightBorder"
                                        label="Cancel"
                                        type="button"
                                    />
                                </AlertDialogPrimitive.Cancel>

                                <AlertDialogPrimitive.Action asChild={true}>
                                    <Button
                                        displayType={
                                            danger ? 'danger' : 'primary'
                                        }
                                        label="Delete"
                                        type="submit"
                                        onClick={onConfirmClick}
                                    />
                                </AlertDialogPrimitive.Action>
                            </div>
                        </Content>
                    </Transition.Child>
                </Transition.Root>
            </Portal>
        </Root>
    );
};

export default AlertDialog;
