
FROM openjdk:22-ea-17-slim-bullseye
EXPOSE 8080
RUN mkdir /files && chmod 777 /files
RUN echo "test" > /files/test.txt
COPY target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
