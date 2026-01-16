import {NextRequest, NextResponse} from 'next/server';

export async function GET(request: NextRequest) {
    try {
        const environment = request.headers.get('X-ENVIRONMENT') ?? 'DEVELOPMENT';
        const jwtToken = request.headers.get('Authorization');

        if (!jwtToken) {
            return NextResponse.json({error: 'JWT token is required'}, {status: 401});
        }

        // Forward the request to the ByteChef API
        const response = await fetch('http://localhost:9555/api/embedded/v1/integrations', {
            method: 'GET',
            headers: {
                Authorization: jwtToken,
                'Content-Type': 'application/json',
                'X-ENVIRONMENT': environment,
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            return NextResponse.json(
                {error: errorData.message || 'Failed to fetch integrations from ByteChef API'},
                {status: response.status}
            );
        }

        const integrations = await response.json();

        // Map the response to include only the id and name fields
        const simplifiedIntegrations = integrations.map((integration: any) => ({
            id: integration.id,
            name: integration.name || integration.componentName,
        }));

        return NextResponse.json(simplifiedIntegrations);
    } catch (error) {
        console.error('Error fetching integrations:', error);
        return NextResponse.json({error: 'Failed to fetch integrations'}, {status: 500});
    }
}
