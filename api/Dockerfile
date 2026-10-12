FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/contracts-api-0.1.0.jar app.jar
EXPOSE 8040
ENTRYPOINT ["java","-jar","/app/app.jar"]
