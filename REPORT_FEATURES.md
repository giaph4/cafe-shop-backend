# Coffee Shop Backend - Report Features

## Overview
Comprehensive reporting system for the coffee shop management application with analytics, exports, and dashboard statistics.

## New Report Endpoints

### 1. **Dashboard Statistics** 
`GET /api/v1/reports/dashboard`
- **Description**: Get overall business statistics for today, this month, and this year
- **Response**: 
  - Today/Month/Year revenue and order counts
  - Total customers and products
  - Low stock items count
  - Average order value
  - Today and month profit

### 2. **Top Customers Analytics**
`GET /api/v1/reports/top-customers?startDate={date}&endDate={date}&top={number}`
- **Description**: Get top customers by spending in a date range
- **Parameters**:
  - `startDate`: Start date (YYYY-MM-DD)
  - `endDate`: End date (YYYY-MM-DD)
  - `top`: Number of top customers (default: 10)
- **Response**: Customer ID, name, phone, total orders, total spent, average order value, loyalty points, last order date

### 3. **Staff Performance**
`GET /api/v1/reports/staff-performance?startDate={date}&endDate={date}&top={number}`
- **Description**: Get staff performance metrics by sales
- **Parameters**:
  - `startDate`: Start date (YYYY-MM-DD)
  - `endDate`: End date (YYYY-MM-DD)
  - `top`: Number of top staff (default: 10)
- **Response**: User ID, username, full name, total orders, total revenue, average order value

### 4. **Category Sales Analysis**
`GET /api/v1/reports/category-sales?startDate={date}&endDate={date}`
- **Description**: Get sales breakdown by product category
- **Parameters**:
  - `startDate`: Start date (YYYY-MM-DD)
  - `endDate`: End date (YYYY-MM-DD)
- **Response**: Category ID, name, quantity sold, revenue, revenue percentage, product count

### 5. **Hourly Sales Distribution**
`GET /api/v1/reports/hourly-sales?date={date}`
- **Description**: Get sales distribution by hour of day
- **Parameters**:
  - `date`: Date to analyze (YYYY-MM-DD)
- **Response**: Hour (0-23), order count, revenue, average order value for each hour

### 6. **Payment Method Statistics**
`GET /api/v1/reports/payment-method-stats?startDate={date}&endDate={date}`
- **Description**: Get revenue breakdown by payment method
- **Parameters**:
  - `startDate`: Start date (YYYY-MM-DD)
  - `endDate`: End date (YYYY-MM-DD)
- **Response**: Payment method, order count, total amount, percentage

### 7. **Sales Comparison**
`GET /api/v1/reports/sales-comparison?currentStart={date}&currentEnd={date}&previousStart={date}&previousEnd={date}`
- **Description**: Compare sales between two periods
- **Parameters**:
  - `currentStart`: Current period start date
  - `currentEnd`: Current period end date
  - `previousStart`: Previous period start date
  - `previousEnd`: Previous period end date
- **Response**: Current/previous revenue and orders, growth amount, growth percentage

### 8. **Export Inventory to Excel**
`GET /api/v1/reports/inventory/export`
- **Description**: Export current inventory to Excel file
- **Response**: Excel file with inventory details (ID, name, unit, quantity, reorder level, unit price, total value, status)

### 9. **Export Expenses to Excel**
`GET /api/v1/reports/expenses/export?startDate={date}&endDate={date}`
- **Description**: Export expenses in date range to Excel file
- **Parameters**:
  - `startDate`: Start date (YYYY-MM-DD)
  - `endDate`: End date (YYYY-MM-DD)
- **Response**: Excel file with expense details

## Existing Report Endpoints

### 10. **Daily Revenue**
`GET /api/v1/reports/daily-revenue?date={date}`
- Get total revenue for a specific date

### 11. **Inventory Report**
`GET /api/v1/reports/inventory?lowStock={boolean}`
- Get current inventory or low stock items

### 12. **Export Orders to Excel**
`GET /api/v1/reports/orders/export?startDate={date}&endDate={date}`
- Export orders in date range to Excel

### 13. **Profit Report**
`GET /api/v1/reports/profit?startDate={date}&endDate={date}`
- Get profit analysis (revenue, cost of goods sold, profit)

### 14. **Best Sellers**
`GET /api/v1/reports/best-sellers?startDate={date}&endDate={date}&top={number}&sortBy={quantity|revenue}`
- Get top selling products

### 15. **Revenue by Date Range**
`GET /api/v1/reports/revenue-by-date?startDate={date}&endDate={date}`
- Get daily revenue breakdown for date range

### 16. **Expenses by Date Range**
`GET /api/v1/reports/expenses-by-date?startDate={date}&endDate={date}`
- Get daily expenses breakdown by category

## Security
All report endpoints require `MANAGER` or `ADMIN` role.

## Database Enhancements

### New Repository Methods:
- **OrderRepository**: 
  - `countTodayOrders()`, `countMonthOrders()`, `countYearOrders()`
  - `sumMonthRevenue()`, `sumYearRevenue()`
  - `findTodayPaidOrders()`

- **CustomerRepository**:
  - `findTopCustomersBetweenDates()` - Analytics query with aggregations

- **UserRepository**:
  - `findStaffPerformanceBetweenDates()` - Staff performance metrics

- **OrderDetailRepository**:
  - `findCategorySalesBetweenDates()` - Category sales analysis

## DTOs Used
- `DashboardStatsDTO`
- `CustomerAnalyticsDTO`
- `StaffPerformanceDTO`
- `CategorySalesDTO`
- `HourlySalesDTO`
- `PaymentMethodStatsDTO`
- `SalesComparisonDTO`
- `BestSellerDTO`
- `IngredientResponseDTO`

## Usage Examples

### Get Dashboard Stats
```
GET /api/v1/reports/dashboard
Authorization: Bearer {token}
```

### Get Top 5 Customers This Month
```
GET /api/v1/reports/top-customers?startDate=2025-11-01&endDate=2025-11-30&top=5
Authorization: Bearer {token}
```

### Compare This Week vs Last Week
```
GET /api/v1/reports/sales-comparison?currentStart=2025-11-04&currentEnd=2025-11-10&previousStart=2025-10-28&previousEnd=2025-11-03
Authorization: Bearer {token}
```

### Export Inventory
```
GET /api/v1/reports/inventory/export
Authorization: Bearer {token}
```

## Notes
- All date parameters use ISO format (YYYY-MM-DD)
- Excel exports include formatted headers and auto-sized columns
- Dashboard stats are calculated in real-time
- All monetary values use BigDecimal for precision
- Percentages are calculated with 4 decimal places precision
