FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY frontend frontend
RUN mkdir -p backend/src/main/resources/static \
    && cp -r frontend/. backend/src/main/resources/static/
WORKDIR /workspace/backend
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/target/*.jar app.jar
ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/pixfactory?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
ENV SPRING_DATASOURCE_USERNAME=pixfactory
ENV SPRING_DATASOURCE_PASSWORD=pixfactory
ENV FRONTEND_DIR=
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
