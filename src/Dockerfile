FROM  maven:3-eclipse-temurin-17 AS build 
copy . .
RUN mvn clean package -DskipTest
FROM  eclipse-temurin:17-alpine
copy --from=build /target/*.jar demo.jar
expose 8080
entrypoint ["java", "-jar","demo.jar"]