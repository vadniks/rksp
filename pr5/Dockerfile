
FROM openjdk:22-ea-17-slim-bullseye
COPY target/*.jar /app/app.jar
RUN mkdir /files && chmod 777 /files
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
