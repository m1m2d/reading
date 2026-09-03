# 国内基础镜像（默认 DaoCloud 加速，可通过 --build-arg BASE_REGISTRY 覆盖）
ARG BASE_REGISTRY=docker.m.daocloud.io/library
FROM ${BASE_REGISTRY}/node:20-alpine AS build
WORKDIR /app
# 使用 npmmirror 加速 npm 依赖安装
RUN npm config set registry https://registry.npmmirror.com
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
RUN npm run build

# 运行阶段：Nginx 托管静态资源并反向代理后端
ARG BASE_REGISTRY=docker.m.daocloud.io/library
FROM ${BASE_REGISTRY}/nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
