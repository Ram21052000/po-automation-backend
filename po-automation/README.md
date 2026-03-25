# PO Automation Prototype

Full-stack prototype to automate purchase order extraction from PDF files, store structured data, expose analytics APIs, and power a real-time dashboard.

## Tech Stack

- Backend: Spring Boot, Spring Security, Spring Data JPA, MySQL, PDFBox
- Frontend: React, Axios, Chart.js
- Database: MySQL (`po_db`)

## Features Implemented

- PDF upload and extraction (`/api/po/upload`)
- Standardized PO fields:
  - Supplier, Brand, Buyer, Category, Style Number
  - Order Quantity, Unit Price, Total Amount, Currency
  - Confirmed Ex-Factory Date, Actual Delivery Date, Delivery Status
- Parse validation with error capture (`parseStatus`, `parseErrors`)
- Relational persistence in MySQL
- Secure APIs via Basic Authentication
- Filtered query endpoint (date range, supplier, buyer, category)
- Insights endpoints:
  - Order count/value/quantity by supplier and brand
  - Delivery timeline
- CSV export endpoint
- USD->GBP conversion endpoint with live API + fallback cache
- Frontend dashboard with upload, filters, KPI cards, insights chart, and PO table

## Architecture

`File Upload -> PDF Parser -> Validation/Normalization -> MySQL -> REST APIs -> React Dashboard`

## Backend Setup

1. Update DB credentials in `src/main/resources/application.properties`.
2. Start MySQL locally.
3. Run:
   - `./mvnw spring-boot:run` (Linux/Mac)
   - `mvnw.cmd spring-boot:run` (Windows)
4. Default API auth:
   - Username: `admin`
   - Password: `admin123`

## Frontend Setup

1. Open `C:\Users\ASUS\Desktop\po-frontend`
2. Run:
   - `npm install`
   - `npm start`
3. Open `http://localhost:3000`

## API List

- `POST /api/po/upload`
- `GET /api/po`
- `GET /api/po/kpis`
- `GET /api/po/insights/supplier-brand`
- `GET /api/po/insights/delivery-timeline`
- `GET /api/po/currency/usd-to-gbp`
- `GET /api/po/export`
- `GET /api/po/health` (public)

## Demo Flow

1. Start backend and frontend.
2. Upload a PO PDF.
3. Validate parsed data in table.
4. Apply filters.
5. Review KPI cards and supplier-brand chart.
6. Export records as CSV.
7. Show live/fallback currency rate.

## Notes

- Parser is template-tolerant with label-based regex extraction and safe fallbacks.
- For enterprise use, extend to OCR for scanned PDFs and multi-template parser strategies.
