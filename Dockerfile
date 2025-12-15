FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY . .

# Убедимся, что gradlew исполняемый
RUN chmod +x ./gradlew

# Установим зависимости и соберём проект
RUN ./gradlew build -x test

# Запускаем JAR (любой в build/libs)
CMD ["sh", "-c", "java -jar build/libs/*.jar"]