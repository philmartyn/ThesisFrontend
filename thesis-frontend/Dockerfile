FROM java:8-alpine
RUN mkdir -p /app /app/resources
WORKDIR /app
COPY target/*-standalone.jar .
COPY resources/public resources/public
CMD java -jar thesis-frontend-0.1.0-standalone.jar
EXPOSE 3006
EXPOSE 5672