/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,
    transpilePackages: ['@bytechef/chat'],
    experimental: {
        optimizePackageImports: ['@bytechef/chat'],
    },
    turbopack: {},
};

export default nextConfig;
