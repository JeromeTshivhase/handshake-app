# Stage 1: build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=6 -Dmaven.wagon.http.pool=false dependency:go-offline
RUN for i in 1 2 3; do \
      mvn -B -ntp clean package -Dmaven.test.skip=true \
          -Dmaven.wagon.http.retryHandler.count=6 -Dmaven.wagon.http.pool=false && break; \
      echo "Maven package failed (attempt $i), retrying..."; \
      sleep 5; \
    done

# Stage 2: runtime (JRE only — no build tools in production image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/handshake.jar app.jar
# NOTE: Do NOT copy the uploads/ directory — user files must be stored in
#       Cloudinary. The local ./uploads path is only used as a dev fallback.
EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
