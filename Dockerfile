FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY . /app

RUN ./mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
