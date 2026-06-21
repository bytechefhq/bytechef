# ByteChef Next.js App with Server-side JWT Generation

This is a Next.js application that demonstrates how to use the ByteChef embedded React library with server-side JWT token generation.

## Key Features

- Uses Next.js App Router for modern React development
- Generates JWT tokens on the server side using the `jsonwebtoken` library
- Fetches available integrations from the ByteChef API and displays them in a select dropdown
- Provides a user interface similar to the original React app but with server-side processing
- Demonstrates how to use the ByteChef embedded React library in a Next.js application

## Why Server-side JWT Generation?

The `jsonwebtoken` library is a Node.js-only library and cannot be used directly in the browser. By moving the JWT generation to a server-side API route, we can securely generate tokens without exposing private keys to the client.

## Getting Started

1. Install dependencies:

```bash
npm install
```

2. Run the development server:

```bash
npm run dev
```

3. Open [http://localhost:3000](http://localhost:3000) in your browser.

## How It Works

1. The user enters their Key ID, Private Key, External User ID, and Name in the form.
2. When the user clicks "Calculate JWT Token", the application sends a request to the server-side API route `/api/generate-jwt`.
3. The server-side API route uses the `jsonwebtoken` library to generate a JWT token and returns it to the client.
4. Once the JWT token is generated, the application automatically fetches available integrations from the ByteChef API using the `/api/integrations` endpoint.
5. The integrations are displayed in a select dropdown, allowing the user to choose which integration to connect to.
6. The user selects an integration and clicks "Connect" to open the ByteChef connect dialog.

## API Routes

### `/api/generate-jwt`

- **Method**: POST
- **Request Body**:
    ```json
    {
        "kid": "your-key-id",
        "privateKey": "your-private-key",
        "externalUserId": "user-id",
        "name": "User Name"
    }
    ```
- **Response**:
    ```json
    {
        "token": "generated-jwt-token"
    }
    ```

### `/api/integrations`

- **Method**: GET
- **Headers**:
    ```
    Authorization: Bearer your-jwt-token
    ```
- **Response**:
    ```json
    [
        {
            "id": 1,
            "name": "Integration Name"
        },
        {
            "id": 2,
            "name": "Another Integration"
        }
    ]
    ```
- **Description**: This endpoint proxies the request to the ByteChef API to fetch available integrations. It requires a valid JWT token in the Authorization header.
