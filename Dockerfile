# 阶段 1：在容器内编译，他人电脑无需安装 JDK/Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY erp-parents erp-parents
COPY erp-pojo erp-pojo
COPY erp-utils erp-utils
COPY erp-server erp-server

RUN mvn clean package -Dmaven.test.skip=true

# 阶段 2：运行
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/erp-server/target/erp-server-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
