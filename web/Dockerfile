FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
EXPOSE 3040
ENV API_PROXY=http://api:8040
CMD ["npm", "run", "dev"]
