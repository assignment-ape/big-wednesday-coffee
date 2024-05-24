FROM openjdk:17-alpine

WORKDIR /app

COPY build/libs/WaveDays-1.0.0.jar /app/WaveDays-1.0.0.jar

CMD ["java", "-jar", "WaveDays-1.0.0.jar"]