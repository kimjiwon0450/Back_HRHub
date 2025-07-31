# ─── 빌드 스테이지 ───────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY . .
RUN chmod +x ./gradlew \
 && ./gradlew clean build -x test

# ─── 런타임 스테이지 ─────────────────────────
FROM bellsoft/liberica-openjdk-alpine:17
WORKDIR /app

# curl 설치
RUN apk add --no-cache curl

# 빌드된 JAR 복사 및 실행
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
