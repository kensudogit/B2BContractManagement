/** @type {import('next').NextConfig} */
const api = process.env.API_PROXY || "http://127.0.0.1:8040";

const nextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${api}/api/:path*` }];
  },
};

export default nextConfig;
