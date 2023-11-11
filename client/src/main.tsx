import React from 'react';
import {createRoot} from 'react-dom/client';

import {worker} from './mocks/server';

import './styles/index.css';

import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactQueryDevtools} from '@tanstack/react-query-devtools';
import {RouterProvider} from 'react-router-dom';

import {router} from './routes';

if (process.env.NODE_ENV === 'mock') {
    worker.start().then(() => renderApp());
} else {
    renderApp();
}

function renderApp() {
    const container = document.getElementById('root') as HTMLDivElement;
    const root = createRoot(container);
    const queryClient = new QueryClient();

    root.render(
        <React.StrictMode>
            <QueryClientProvider client={queryClient}>
                <RouterProvider router={router} />

                <ReactQueryDevtools
                    buttonPosition="bottom-right"
                    initialIsOpen={false}
                />
            </QueryClientProvider>
        </React.StrictMode>
    );
}
