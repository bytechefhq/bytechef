import {KeyboardEvent} from 'react';

export const handleActivationKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (event.target !== event.currentTarget || (event.key !== 'Enter' && event.key !== ' ')) {
        return;
    }

    event.preventDefault();

    event.currentTarget.click();
};
