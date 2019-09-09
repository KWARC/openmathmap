FROM maven AS build  
ADD src /usr/src/app/src  
ADD WebContent /usr/src/app/WebContent/
ADD pom.xml /usr/src/app  
RUN mvn -f /usr/src/app/pom.xml clean package

FROM jetty

EXPOSE 8080
COPY --from=build /usr/src/app/target/MathService-1.0-SNAPSHOT.war /var/lib/jetty/webapps/MathService.war