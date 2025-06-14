import {NextRequest, NextResponse} from 'next/server';
import jwt from 'jsonwebtoken';

export async function POST(request: NextRequest) {
    try {
        // Parse the request body
        const {kid, privateKey, externalUserId, name} = await request.json();

        // Validate required fields
        if (!kid || !privateKey || !externalUserId || !name) {
            return NextResponse.json({error: 'Missing required fields'}, {status: 400});
        }

        // Create JWT payload with standard claims
        const payload = {
            sub: externalUserId,
            name: name || externalUserId,
            iat: Math.floor(Date.now() / 1000),
        };

        // Generate the JWT token
        const token = jwt.sign(payload, privateKey, {
            algorithm: 'RS256',
            expiresIn: '1h',
            keyid: kid,
        });

        // Return the token
        return NextResponse.json({token});
    } catch (error) {
        console.error('Error generating JWT token:', error);
        return NextResponse.json({error: 'Failed to generate JWT token'}, {status: 500});
    }
}
