# # ---- Build stage ----
# FROM eclipse-temurin:17-jdk AS builder
# WORKDIR /app
#
# COPY pom.xml ./
# COPY mvnw ./
# COPY .mvn .mvn
#
# COPY src ./src
#
# RUN chmod +x ./mvnw && ./mvnw -q -DskipTests package
#
# # ---- Runtime stage ----
# FROM eclipse-temurin:17-jre
# WORKDIR /app
#
# COPY --from=builder /app/target/*.jar app.jar
#
# # Render injects the actual PORT at runtime; application.properties uses ${PORT:8080}
# EXPOSE 8080
#
# ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

FROM openjdk:17
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]