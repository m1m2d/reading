# 国内基础镜像（默认 DaoCloud 加速，可通过 --build-arg BASE_REGISTRY 覆盖）
ARG BASE_REGISTRY=docker.m.daocloud.io/library
FROM ${BASE_REGISTRY}/eclipse-temurin:17-jdk AS build
WORKDIR /build
# 轻量 Maven：使用随项目携带的二进制（约 10MB），替代 800MB 的 maven 镜像
COPY docker-maven /opt/maven
RUN ln -s /opt/maven/bin/mvn /usr/local/bin/mvn
# 使用阿里云 Maven 镜像加速依赖下载
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -DskipTests package

# 运行阶段
ARG BASE_REGISTRY=docker.m.daocloud.io/library
FROM ${BASE_REGISTRY}/eclipse-temurin:17-jre
WORKDIR /app
ENV APP_DATA_DIR=/app/data
COPY --from=build /build/target/cloudread-backend-1.0.0.jar app.jar
EXPOSE 8080
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "app.jar"]
