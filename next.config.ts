import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // No native modules needed (using @libsql/client instead of better-sqlite3)
};

export default nextConfig;
