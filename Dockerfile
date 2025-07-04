# ------------------------------
# Build stage
# ------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests


# ------------------------------
# Runtime stage
# ------------------------------
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Nhận build arg từ GitHub Actions
ARG FIREBASE_KEY_BASE64

# Biến môi trường cho Firebase path (sẽ được Spring Boot dùng)
ENV FIREBASE_CREDENTIAL_PATH=/app/firebase/firebase.json

# Tạo thư mục chứa key
RUN mkdir -p /app/firebase

# Ghi file firebase.json từ biến base64 (nếu có)
RUN echo "$FIREBASE_KEY_BASE64" | base64 -d > /app/firebase/firebase.json

# Copy JAR từ stage build
COPY --from=build /app/target/*.jar app.jar

# Expose port và run app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-XX:+UseG1GC", "-jar", "/app/app.jar"]
