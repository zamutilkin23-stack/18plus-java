FROM openjdk:17-slim

WORKDIR /app

COPY . .

# Сделать gradlew исполняемым и собрать проект
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# Запустить JAR (любой в папке build/libs)
CMD ["sh", "-c", "java -jar build/libs/*.jar"]