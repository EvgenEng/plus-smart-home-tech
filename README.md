# Smart Home Technologies - Интернет-магазин умного дома

Полнофункциональная микросервисная платформа для интернет-магазина умной домашней техники и систем автоматизации.

## 🏗️ Архитектура

Проект построен по микросервисной архитектуре с использованием Spring Cloud и состоит из трех основных доменов:

### 🛒 Commerce
- **shopping-store** - Каталог товаров и управление ассортиментом
- **shopping-cart** - Корзина покупок
- **warehouse** - Складской учет и управление запасами
- **order** - Управление заказами (13 статусов жизненного цикла)
- **payment** - Эмулятор платежного шлюза с расчетом налогов и стоимости
- **delivery** - Эмулятор службы доставки со сложным алгоритмом расчета

### 🏠 Telemetry
- **collector** - Сбор данных с датчиков устройств
- **aggregator** - Агрегация событий умного дома
- **analyzer** - Анализ данных и выполнение сценариев автоматизации

### 🔧 Infra
- **discovery-server** - Service Discovery (Eureka)
- **config-server** - Централизованная конфигурация
- **gateway** - API Gateway с Circuit Breaker

## 🚀 Технологический стек

- **Java 17** - Основной язык разработки
- **Spring Boot 3.0** - Фреймворк
- **Spring Cloud** - Микросервисные паттерны
- **PostgreSQL** - Реляционные базы данных
- **Spring Data JPA** - Доступ к данным
- **Spring Cloud OpenFeign** - REST клиенты
- **Eureka** - Service Discovery
- **Spring Cloud Config** - Централизованная конфигурация
- **Spring Cloud Gateway** - API Gateway
- **Resilience4j** - Circuit Breaker и отказоустойчивость
- **Lombok** - Уменьшение boilerplate кода
- **Maven** - Сборка и управление зависимостями

## 📋 Функциональность

### Электронная коммерция
- 📦 **Shopping Store** - Управление каталогом товаров умного дома
- 🛒 **Shopping Cart** - Корзина покупок с валидацией доступности
- 📦 **Warehouse** - Складской учет, бронирование товаров, управление запасами
- 📋 **Order Service** - Полный жизненный цикл заказа с 13 статусами:
  - `NEW` → `ON_PAYMENT` → `ON_DELIVERY` → `COMPLETED`
  - Обработка ошибок: `PAYMENT_FAILED`, `DELIVERY_FAILED`, `ASSEMBLY_FAILED`
  - Возвраты: `PRODUCT_RETURNED`, `CANCELED`
- 💳 **Payment Service** - Эмулятор платежного шлюза:
  - Расчет стоимости товаров
  - Расчет НДС (10%)
  - Интеграция с заказами
- 🚚 **Delivery Service** - Эмулятор службы доставки:
  - Сложный алгоритм расчета стоимости
  - Учет веса, объема, хрупкости товаров
  - Управление статусами доставки

### Умный дом
- 📡 **Collector** - Сбор данных с датчиков (движение, температура, освещенность)
- 🧠 **Aggregator & Analyzer** - Автоматизация через сценарии
- ⚡ Обработка событий в реальном времени
- 🔌 Управление устройствами умного дома

## 🏃‍♂️ Быстрый старт

### Предварительные требования
- Java 17
- Maven 3.6+
- PostgreSQL 12+

### Запуск проекта

1. **Клонирование репозитория**

git clone https://github.com/your-username/plus-smart-home-tech.git
cd plus-smart-home-tech

2. **Создание баз данных**

CREATE DATABASE shopping_cart_db;
CREATE DATABASE shopping_store_db;
CREATE DATABASE warehouse_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE delivery_db;
CREATE DATABASE analyzer_db;

3. **Запуск инфраструктурных сервисов**

## Запуск Discovery Server
cd infra/discovery-server
mvn spring-boot:run

## Запуск Config Server (в новом терминале)
cd infra/config-server
mvn spring-boot:run

## Запуск Gateway (в новом терминале)
cd infra/gateway
mvn spring-boot:run

4. **Запуск бизнес-сервисов**

## Commerce сервисы
cd commerce/shopping-store && mvn spring-boot:run
cd commerce/shopping-cart && mvn spring-boot:run
cd commerce/warehouse && mvn spring-boot:run
cd commerce/order && mvn spring-boot:run
cd commerce/payment && mvn spring-boot:run
cd commerce/delivery && mvn spring-boot:run

## Telemetry сервисы
cd telemetry/collector && mvn spring-boot:run
cd telemetry/aggregator && mvn spring-boot:run
cd telemetry/analyzer && mvn spring-boot:run

5. **Доступ к сервисам**

Eureka Dashboard: http://localhost:8761

API Gateway: http://localhost:8080

Config Server: http://localhost:8888

6. **Конфигурация**

Основные настройки

Порт Eureka: 8761
Порт Config Server: 8888
Порт Gateway: 8080
Базы данных: PostgreSQL на порту 5432
