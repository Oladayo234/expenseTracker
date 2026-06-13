# Stage 1: Build
FROM ubuntu:24.04 AS builder
RUN apt-get update && apt-get install -y maven openjdk-25-jdk && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -DskipTests -B -q

# Stage 2: Run
FROM ubuntu:24.04
RUN apt-get update && apt-get install -y openjdk-25-jre && rm -rf /var/lib/apt/lists/*

# Non-root user for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]