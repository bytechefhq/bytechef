import {useConnectDialog} from '@bytechef/embedded-react';
import React, { useState, useCallback } from 'react';

function App() {
  const [kid, setKid] = useState('');
  const [privateKey, setPrivateKey] = useState('');
  const [integrationId, setIntegrationId] = useState('');
  const [jwtToken, setJwtToken] = useState('');

  // Function to calculate JWT token
  const calculateJwtToken = useCallback(async () => {
    if (!kid || !privateKey || !integrationId) {
      alert('Please fill in all fields');
      return;
    }

    try {
      // Create JWT header
      const header = {
        alg: 'RS256',
        typ: 'JWT',
        kid: kid
      };

      // Create JWT payload with standard claims
      const now = Math.floor(Date.now() / 1000);
      const payload = {
        iat: now,
        exp: now + 3600, // Token expires in 1 hour
        sub: integrationId
      };

      // Base64Url encode header and payload
      const base64UrlHeader = btoa(JSON.stringify(header))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');

      const base64UrlPayload = btoa(JSON.stringify(payload))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');

      // Create the content to be signed
      const content = `${base64UrlHeader}.${base64UrlPayload}`;

      // For demonstration purposes, we'll set a placeholder token
      // In a real implementation, you would use Web Crypto API to sign the content with the private key
      const token = `${content}.SIGNATURE`;

      setJwtToken(token);
      alert('JWT Token calculated! You can now connect.');
    } catch (error) {
      console.error('Error calculating JWT token:', error);
      alert('Error calculating JWT token. Check console for details.');
    }
  }, [kid, privateKey, integrationId]);

  const {openDialog} = useConnectDialog({
    baseUrl: 'http://localhost:9555',
    integrationId: integrationId,
    jwtToken: jwtToken
  });

  const handleConnect = () => {
    if (!jwtToken) {
      alert('Please calculate JWT token first');
      return;
    }
    openDialog();
  };

  return (
    <div style={{ padding: '20px', maxWidth: '600px', margin: '0 auto' }}>
      <h1>ByteChef Connection</h1>

      <div style={{ marginBottom: '15px' }}>
        <label style={{ display: 'block', marginBottom: '5px' }}>
          Key ID (kid):
          <input
            type="text"
            value={kid}
            onChange={(e) => setKid(e.target.value)}
            style={{
              display: 'block',
              width: '100%',
              padding: '8px',
              marginTop: '5px',
              borderRadius: '4px',
              border: '1px solid #ccc'
            }}
            placeholder="Enter Key ID"
          />
        </label>
      </div>

      <div style={{ marginBottom: '15px' }}>
        <label style={{ display: 'block', marginBottom: '5px' }}>
          Private Key:
          <textarea
            value={privateKey}
            onChange={(e) => setPrivateKey(e.target.value)}
            style={{
              display: 'block',
              width: '100%',
              padding: '8px',
              marginTop: '5px',
              borderRadius: '4px',
              border: '1px solid #ccc',
              minHeight: '150px',
              fontFamily: 'monospace'
            }}
            placeholder="Enter Private Key (PEM format)"
          />
        </label>
      </div>

      <div style={{ marginBottom: '15px' }}>
        <label style={{ display: 'block', marginBottom: '5px' }}>
          Integration ID:
          <input
            type="text"
            value={integrationId}
            onChange={(e) => setIntegrationId(e.target.value)}
            style={{
              display: 'block',
              width: '100%',
              padding: '8px',
              marginTop: '5px',
              borderRadius: '4px',
              border: '1px solid #ccc'
            }}
            placeholder="Enter Integration ID"
          />
        </label>
      </div>

      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        <button
          onClick={calculateJwtToken}
          style={{
            padding: '10px 15px',
            backgroundColor: '#4CAF50',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Calculate JWT Token
        </button>

        <button
          onClick={handleConnect}
          style={{
            padding: '10px 15px',
            backgroundColor: '#2196F3',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Connect
        </button>
      </div>

      {jwtToken && (
        <div style={{ marginTop: '20px' }}>
          <h3>Generated JWT Token:</h3>
          <div
            style={{
              padding: '10px',
              backgroundColor: '#f5f5f5',
              borderRadius: '4px',
              wordBreak: 'break-all',
              fontFamily: 'monospace'
            }}
          >
            {jwtToken}
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
