/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,
    transpilePackages: ['@bytechef/embedded-react'],
    experimental: {
        optimizePackageImports: ['@bytechef/embedded-react'],
    },
    turbopack: {},
};

export default nextConfig;
