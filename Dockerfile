# one image per module: docker build --build-arg MODULE=workers/settlement_node .
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    : "${MODULE:?set MODULE, e.g. gateway}" \
    && mvn -B -q -pl "${MODULE}" -am package -DskipTests \
    && cp "${MODULE}"/target/*.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home app
COPY --from=build /app.jar /app/app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
