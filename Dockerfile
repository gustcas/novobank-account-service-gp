FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY checkstyle.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S novobanco && adduser -S novobanco -G novobanco
COPY --from=builder /build/target/*.jar app.jar
USER novobanco
ENTRYPOINT ["java", "-jar", "app.jar"]
