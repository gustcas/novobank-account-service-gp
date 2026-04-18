FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY checkstyle.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S novobanco && adduser -S novobanco -G novobanco
COPY --from=builder /build/target/*.jar app.jar
USER novobanco
ENTRYPOINT ["java", "-jar", "app.jar"]
