FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/enterprisesearch-3.3.0.jar app.jar
EXPOSE 5004
ENTRYPOINT ["java", "-jar", "app.jar"]