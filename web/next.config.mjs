/** @type {import('next').NextConfig} */
const api = process.env.API_PROXY || "http://127.0.0.1:8040";

const nextConfig = {
  output: "standalone",
  reactStrictMode: true,
  allowedDevOrigins: ["127.0.0.1", "localhost"],
  webpack: (config, { dev }) => {
    if (dev) {
      config.watchOptions = { poll: 1000, aggregateTimeout: 300 };
    }
    return config;
  },
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${api}/api/:path*` }];
  },
};

export default nextConfig;
