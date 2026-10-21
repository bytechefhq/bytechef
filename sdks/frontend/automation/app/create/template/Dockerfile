FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then \
        npm ci || { \
            echo 'package-lock.json does not match this npm version; falling back to npm install (locked versions are kept)'; \
            npm install --no-audit --no-fund; \
        }; \
    else \
        npm install --no-audit --no-fund; \
    fi
COPY . .
RUN npm run build && node scripts/check-bundle.mjs

FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/.output ./.output
USER node
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
