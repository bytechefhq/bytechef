import {createStart} from '@tanstack/react-start';

import {requestMiddleware} from './server/bytechef';

export const startInstance = createStart(() => ({requestMiddleware}));
