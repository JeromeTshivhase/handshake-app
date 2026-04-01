# Use Maven to build the app, then run with JRE only
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=6 -Dmaven.wagon.http.pool=false dependency:go-offline
RUN for i in 1 2 3; do \
	  mvn -B -ntp clean package -Dmaven.test.skip=true -Dmaven.wagon.http.retryHandler.count=6 -Dmaven.wagon.http.pool=false && break; \
	  echo "Maven package failed (attempt $i), retrying..."; \
	  sleep 5; \
	done

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/handshake-0.0.1-SNAPSHOT.jar app.jar
COPY uploads ./uploads
EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

