# build stage
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace

# build dependency cache
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline


# copy source code and build, skip tests (will be run in CI and need Docker to run)
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# runtime
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# copy build jar
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

# run as non-root user
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
