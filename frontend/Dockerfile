# Stage 1: build
FROM node:20-alpine AS build

WORKDIR /app

# Cache de dependencias: se re-descarga solo si cambia package-lock.json
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npx ng build --configuration production

# Stage 2: runtime
FROM nginx:alpine AS runtime

COPY --from=build /app/dist/consultorio-odontologico/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
