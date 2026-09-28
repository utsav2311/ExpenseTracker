# Stage 1: Build Core Java application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .

# Compile Core Java source files into bin/ directory
RUN mkdir -p bin && javac -d bin -cp "src:lib/*" $(find src -name "*.java")

# Stage 2: Minimal JRE Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled bytecode and required runtime assets
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
COPY --from=builder /app/web ./web
COPY --from=builder /app/db.properties ./db.properties
COPY --from=builder /app/schema.sql ./schema.sql

# Expose default HTTP port (Render/Railway injects PORT environment variable)
ENV PORT=8080
EXPOSE 8080

# Launch Core Java HttpServer with the configured port
CMD ["sh", "-c", "java -cp 'bin:lib/*' com.expensetracker.Main web ${PORT:-8080}"]
