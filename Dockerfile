# Etapa 1: Compilacion
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final de ejecucion
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 5000
ENV PORT=5000

ENTRYPOINT ["java", "-jar", "app.jar"]