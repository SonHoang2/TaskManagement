# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
# common-lib is a local dependency: install it first (build context = Backend/)
COPY pom.xml ./pom.xml
COPY common-lib ./common-lib
RUN mvn -q -N install -DskipTests && mvn -q -f common-lib/pom.xml install -DskipTests -Dmaven.test.skip=true

WORKDIR /app
COPY task-service/pom.xml .
COPY task-service/src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 5003
ENTRYPOINT ["java", "-jar", "app.jar"]
