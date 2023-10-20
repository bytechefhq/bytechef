/* eslint-disable tailwindcss/no-custom-classname */

import {
    Close,
    Content,
    Description,
    Overlay,
    Portal,
    Title,
    Trigger,
    Root,
} from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import './Modal.scss';

const Modal: React.FC<{
    cancelButtonLabel: string;
    children: React.ReactNode;
    confirmButtonLabel: string;
    description: string;
    triggerLabel: string;
    title: string;
}> = ({
    cancelButtonLabel,
    children,
    confirmButtonLabel,
    description,
    triggerLabel,
    title,
}) => (
    <Root>
        <Trigger asChild>
            <button className="Button violet">{triggerLabel}</button>
        </Trigger>

        <Portal>
            <Overlay className="DialogOverlay" />

            <Content className="DialogContent">
                <Title className="DialogTitle">{title}</Title>

                <Description className="DialogDescription">
                    {description}
                </Description>

                {children}

                <div
                    style={{
                        display: 'flex',
                        marginTop: 25,
                        justifyContent: 'flex-end',
                    }}
                >
                    <Close asChild>
                        <button className="Button white">
                            {cancelButtonLabel}
                        </button>
                    </Close>

                    <Close asChild>
                        <button className="Button green">
                            {confirmButtonLabel}
                        </button>
                    </Close>
                </div>

                <Close asChild>
                    <button className="IconButton" aria-label="Close">
                        <Cross2Icon />
                    </button>
                </Close>
            </Content>
        </Portal>
    </Root>
);

export default Modal;
