FROM  maven:3-eclipse-temurin-17 AS build 
copy . .
RUN mvn clean package -DskipTests
FROM  eclipse-temurin:17-alpine
copy --from=build /target/employeeAtt-0.0.1-SNAPSHOT.jar demo.jar
expose 8080
entrypoint ["java", "-jar","demo.jar"]