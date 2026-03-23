import type { NextConfig } from "next";
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./i18n/request.ts');

const nextConfig: NextConfig = {
  // No native modules needed (using @libsql/client instead of better-sqlite3)

  // Proxy API calls to Scala backend when SCALA_BACKEND is set
  async rewrites() {
    const scalaBackend = process.env.SCALA_BACKEND;
    if (!scalaBackend) return [];
    return [
      {
        source: '/api/:path*',
        destination: `${scalaBackend}/api/:path*`,
      },
    ];
  },
};

export default withNextIntl(nextConfig);
