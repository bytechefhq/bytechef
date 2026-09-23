'use client';

import {AutomationHub} from '@bytechef/embedded';
import Link from 'next/link';
import {useState} from 'react';
import TokenForm, {type Environment} from '../components/TokenForm';

export default function Hub() {
    const [jwtToken, setJwtToken] = useState('');
    const [baseUrl, setBaseUrl] = useState('http://127.0.0.1:5173');
    const [environment, setEnvironment] = useState<Environment>('DEVELOPMENT');

    return (
        <main style={{maxWidth: 'var(--max-width)', margin: '0 auto'}}>
            <h1>ByteChef Automation Hub</h1>

            <p>
                <Link href="/">&larr; Back to the Connect demo</Link>
            </p>

            {!jwtToken && (
                <TokenForm
                    defaultBaseUrl={baseUrl}
                    defaultEnvironment={environment}
                    onTokenGenerated={(token, baseUrl, environment) => {
                        setJwtToken(token);
                        setBaseUrl(baseUrl);
                        setEnvironment(environment);
                    }}
                />
            )}

            {jwtToken && (
                <AutomationHub
                    baseUrl={baseUrl}
                    className="h-[85vh] w-full border"
                    environment={environment}
                    jwtToken={jwtToken}
                />
            )}
        </main>
    );
}
