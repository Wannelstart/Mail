FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 构建产物在 /app/dist，由 docker-compose 的 nginx 挂载 volume 使用
# 这个 stage 仅用于产出 dist 目录
FROM alpine:3.19
COPY --from=build /app/dist /dist
