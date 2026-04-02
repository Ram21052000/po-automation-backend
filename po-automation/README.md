# PO Automation Prototype

Full-stack prototype to automate purchase order extraction from PDF files, store structured data, expose analytics APIs, and power a real-time dashboard.

## Tech Stack

- Backend: Spring Boot, Spring Security, Spring Data JPA, MySQL, PDFBox
- Frontend: React, Axios, Chart.js
- Database: MySQL (`po_db`)

## Features Implemented

- PDF upload and extraction (`/api/po/upload`)
- Folder ingestion (scan folders recursively for PDFs) (`/api/po/ingest-paths`)
- Standardized PO fields:
  - Supplier, Brand, Buyer, Category, Style Number
  - Order Quantity, Unit Price, Total Amount, Currency
  - Confirmed Ex-Factory Date, Actual Delivery Date, Delivery Status
- Parse validation with error capture (`parseStatus`, `parseErrors`)
- Relational persistence in MySQL
- Secure APIs via Basic Authentication
- Filtered query endpoint (date range, country, supplier, brand, buyer, category, factoryName)
- Insights endpoints:
  - Order count/value/quantity by supplier and brand
  - Delivery timeline
- XLSX export endpoint (template columns) + CSV export
- Currency conversion endpoints with live API + fallback cache
- Adjusted GBP→USD conversion (live rate minus 0.02 as per spec)
- Analytics endpoints (Sep–Aug fiscal year): week/month/year summaries + dimension breakdowns
- Chatbot API + React chatbot UI (text + voice)
- Frontend dashboard with upload, folder ingest, filters, KPI cards, insights charts, analytics tables, and PO line item table

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
- `GET /api/po/currency/gbp-to-usd-adjusted`
- `GET /api/po/export`
- `GET /api/po/export-xlsx`
- `POST /api/po/ingest-paths`
- `GET /api/po/analytics/country`
- `GET /api/po/analytics/time?granularity=week|month|year` (Sep–Aug FY)
- `GET /api/po/analytics/brand-links`
- `GET /api/po/analytics/supplier-links`
- `GET /api/po/analytics/factory-links`
- `GET /api/po/analytics/overall-totals-usd`
- `GET /api/po/analytics/suppliers`
- `GET /api/po/analytics/factories`
- `POST /api/chatbot/ask`
- `GET /api/po/health` (public)

## Demo Flow

1. Start backend and frontend.
2. Either upload a PO PDF OR ingest folders using the UI “Folder Ingestion” panel.
3. Validate parsed line items.
4. Apply filters (country/brand/supplier/factory/time period).
5. Review KPI cards (includes total USD converted using adjusted GBP→USD live-0.02).
6. Review analytics tables (country-wise + week/month/year summaries in Sep–Aug FY).
7. Use chatbot (text or voice) to ask for insights.
8. Export records as XLSX (template columns).

## Notes

- Parser is template-tolerant with label-based regex extraction and safe fallbacks.
- For enterprise use, extend to OCR for scanned PDFs and multi-template parser strategies.
