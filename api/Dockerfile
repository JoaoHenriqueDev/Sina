ARG JAVA_VERSION=21

FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:${JAVA_VERSION}-jre
WORKDIR /app
RUN useradd --system --no-create-home sina
COPY --from=build /app/target/*.jar app.jar
USER sina
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
