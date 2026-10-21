import type {AppCurrentUser} from '@bytechef/app';
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

export interface SignedInUser {
    name: string;
}

export interface SignIn {
    enabled: boolean;
    user: SignedInUser | null;
}

function displayName(user: AppCurrentUser): string {
    const name = [user.firstName, user.lastName].filter((part) => !!part).join(' ');

    return name || user.login;
}

export const getSignIn = createServerFn({method: 'GET'})
    .middleware([bytechef])
    .handler(async ({context}): Promise<SignIn> => {
        if (context.user) {
            return {enabled: true, user: {name: displayName(context.user)}};
        }

        const {userAuthMode} = await context.bytechef.context.get();

        return {enabled: userAuthMode !== 'NONE', user: null};
    });
