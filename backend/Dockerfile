FROM node:22-bookworm-slim AS deps

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm ci

FROM node:22-bookworm-slim AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules

COPY . .

RUN apt-get update -y && apt-get install -y --no-install-recommends openssl ca-certificates && rm -rf /var/lib/apt/lists/*

RUN DATABASE_URL="postgresql://build:build@localhost:5432/build?sslmode=disable" npx prisma generate && npm run build

FROM node:22-bookworm-slim AS runner

WORKDIR /app

ENV NODE_ENV=production

RUN apt-get update -y && apt-get install -y --no-install-recommends openssl ca-certificates && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/prisma ./prisma
COPY package.json ./

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:' + (process.env.PORT || '3000') + '/api/v1/health').then((r) => { if (!r.ok) process.exit(1); }).catch(() => process.exit(1))"

# NOTE: `exec` replaces sh with node so Docker stop (SIGTERM) reaches NestJS
# for graceful shutdown. Without it, PID 1 stays sh and shutdowns hang 10s.
CMD ["sh", "-c", "npx prisma migrate deploy && exec node dist/src/main.js"]
