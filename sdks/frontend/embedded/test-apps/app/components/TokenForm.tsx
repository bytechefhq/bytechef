'use client';

import {useState} from 'react';

export type Environment = 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';

interface TokenFormProps {
    defaultBaseUrl?: string;

    defaultEnvironment?: Environment;

    onBaseUrlChange?: (baseUrl: string) => void;

    onEnvironmentChange?: (environment: Environment) => void;

    onTokenGenerated: (token: string, baseUrl: string, environment: Environment) => void;
}

export default function TokenForm({
    defaultBaseUrl = 'http://127.0.0.1:5173',
    defaultEnvironment = 'DEVELOPMENT',
    onBaseUrlChange,
    onEnvironmentChange,
    onTokenGenerated,
}: TokenFormProps) {
    const [kid, setKid] = useState('');
    const [privateKey, setPrivateKey] = useState('');
    const [name, setName] = useState('John Doe');
    const [externalUserId, setExternalUserId] = useState('1234567890');
    const [baseUrl, setBaseUrl] = useState(defaultBaseUrl);
    const [environment, setEnvironment] = useState<Environment>(defaultEnvironment);
    const [jwtToken, setJwtToken] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    const calculateJwtToken = async () => {
        if (!kid || !privateKey || !externalUserId || !name) {
            alert('Please fill in all fields');
            return;
        }

        setIsLoading(true);
        setError('');

        try {
            const response = await fetch('/api/generate-jwt', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    kid,
                    privateKey,
                    externalUserId,
                    name,
                }),
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Failed to generate JWT token');
            }

            setJwtToken(data.token);
            onTokenGenerated(data.token, baseUrl, environment);
            alert('JWT Token calculated! You can now use it below.');
        } catch (error) {
            console.error('Error calculating JWT token:', error);
            setError(error instanceof Error ? error.message : 'Unknown error occurred');
            alert('Error calculating JWT token. Check console for details.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <>
            <div className="form-group">
                <label>
                    Base URL:
                    <input
                        type="text"
                        value={baseUrl}
                        onChange={(event) => {
                            const nextBaseUrl = event.target.value;

                            setBaseUrl(nextBaseUrl);
                            onBaseUrlChange?.(nextBaseUrl);
                        }}
                        placeholder="Enter Base URL"
                    />
                </label>
            </div>

            <div className="form-group">
                <label>
                    Environment:
                    <select
                        value={environment}
                        onChange={(event) => {
                            const nextEnvironment = event.target.value as Environment;

                            setEnvironment(nextEnvironment);
                            onEnvironmentChange?.(nextEnvironment);
                        }}
                    >
                        <option value="DEVELOPMENT">DEVELOPMENT</option>
                        <option value="STAGING">STAGING</option>
                        <option value="PRODUCTION">PRODUCTION</option>
                    </select>
                </label>
            </div>

            <div className="form-group">
                <label>
                    Private Key:
                    <textarea
                        value={privateKey}
                        onChange={(event) => setPrivateKey(event.target.value)}
                        placeholder="Enter Private Key (PEM format)"
                    />
                </label>
            </div>

            <div className="form-group">
                <label>
                    Key ID (kid):
                    <input
                        type="text"
                        value={kid}
                        onChange={(event) => setKid(event.target.value)}
                        placeholder="Enter Key ID"
                    />
                </label>
            </div>

            <div className="form-group">
                <label>
                    External User ID:
                    <input
                        type="text"
                        value={externalUserId}
                        onChange={(event) => setExternalUserId(event.target.value)}
                        placeholder="Enter External User ID"
                    />
                </label>
            </div>

            <div className="form-group">
                <label>
                    Name:
                    <input
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder="Enter Name"
                    />
                </label>
            </div>

            <div className="button-group">
                <button onClick={calculateJwtToken} className="success" disabled={isLoading}>
                    {isLoading ? 'Calculating...' : 'Calculate JWT Token'}
                </button>
            </div>

            {error && <div style={{color: 'red', marginBottom: '20px'}}>Error: {error}</div>}

            <div className="token-display">
                <h3>Generated JWT Token:</h3>
                <pre>{jwtToken}</pre>
            </div>
        </>
    );
}
