# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Repo layout: Spring Boot app lives in ./po-automation
COPY po-automation/pom.xml ./pom.xml
COPY po-automation/mvnw ./mvnw
COPY po-automation/.mvn ./.mvn
COPY po-automation/src ./src

RUN chmod +x ./mvnw && ./mvnw -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Render injects the actual PORT at runtime; application.properties uses ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "\
  if [ -z \"$JDBC_DATABASE_URL\" ] && [ -n \"$DATABASE_URL\" ]; then \
    # Render often provides DATABASE_URL in the form: postgres://user:pass@host:port/db \
    # Spring expects jdbc:postgresql://host:port/db \
    JDBC_DATABASE_URL=\"jdbc:postgresql://${DATABASE_URL#postgres://}\"; \
    export JDBC_DATABASE_URL; \
  fi; \
  java $JAVA_OPTS -jar /app/app.jar \
"]

