# Build the application
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# Run the application
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60"
EXPOSE 10000

USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--server.address=0.0.0.0", "--server.port=${PORT:10000}"]