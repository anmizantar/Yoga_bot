FROM eclipse-temurin:17-jre

WORKDIR /app

# Копируем JAR и ресурсы
COPY target/Yoga_fact_bot-1.0-SNAPSHOT.jar /app/bot.jar
COPY src/main/resources/yoga_facts.json /app/resources/
COPY src/main/resources/config.properties /app/resources/

CMD ["java", "-Dfile.encoding=UTF-8", "-jar", "bot.jar"]
