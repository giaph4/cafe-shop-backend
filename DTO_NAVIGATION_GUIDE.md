# 🗺️ DTO NAVIGATION GUIDE

**Tổng số:** 60+ DTO files  
**Tình trạng:** Một phần đã grouped, một phần còn loose

---

## 🎯 CÁCH TÌM DTO NHANH

### **Theo Chức Năng:**

#### 🔐 **Authentication & User**
```
dto/
├── LoginRequest.java                    # Login
├── RegisterRequest.java                 # Register
├── AuthenticationResponse.java          # JWT response
├── ChangePasswordRequestDTO.java        # Change password
├── UserResponseDTO.java                 # User profile
├── UserUpdateRequestDTO.java            # Update user
└── RoleDTO.java                         # Role info
```

**Keyword:** `auth`, `login`, `register`, `user`, `password`, `role`

---

#### 🛍️ **Product & Category**
```
dto/
├── ProductRequest.java                  # Create/update product
├── ProductResponse.java                 # Product details
├── ProductRecipeDTO.java                # Recipe
├── ProductIngredientDTO.java            # Ingredients
├── ProductSalesSummaryDTO.java          # Sales stats
├── ProductSalesSummaryResponseDTO.java  # Sales report
│
└── category/                            # ✅ Grouped
    ├── CategoryCreateRequest.java
    ├── CategoryResponse.java
    ├── CategoryUpdateRequest.java
    └── (+ CategorySalesDTO.java - at root, should move here)
```

**Keyword:** `product`, `category`, `recipe`, `ingredient`

---

#### 📋 **Order & Payment**
```
dto/
├── OrderCreateRequestDTO.java           # New order
├── OrderResponseDTO.java                # Order details
├── OrderSummaryDTO.java                 # Order summary
├── OrderDetailRequestDTO.java           # Add item
├── OrderDetailResponseDTO.java          # Item details
├── OrderDetailUpdateRequestDTO.java     # Update item
└── PaymentRequestDTO.java               # Payment
```

**Keyword:** `order`, `payment`, `detail`

---

#### 🪑 **Table Management**
```
dto/
├── CafeTableRequest.java                # Create/update table
├── CafeTableResponse.java               # Table details
└── CafeTableStatusUpdateRequest.java    # Change status
```

**Keyword:** `table`, `cafe`

---

#### 👥 **Customer & Loyalty**
```
dto/
├── CustomerDTO.java                     # Customer profile
├── CustomerAnalyticsDTO.java            # Customer analytics
├── CustomerPurchaseHistoryItemDTO.java  # Purchase item
└── CustomerPurchaseHistoryResponseDTO.java # Full history
```

**Keyword:** `customer`, `purchase`, `history`

---

#### 🎫 **Voucher & Promotions**
```
dto/
├── VoucherRequestDTO.java               # Create/update voucher
├── VoucherResponseDTO.java              # Voucher details
├── VoucherSummaryDTO.java               # Voucher summary
├── VoucherApplyRequestDTO.java          # Apply voucher
└── VoucherCheckResponseDTO.java         # Check validity
```

**Keyword:** `voucher`, `promotion`, `discount`

---

#### 📦 **Inventory & Supply Chain**
```
dto/
├── IngredientRequestDTO.java            # Ingredient CRUD
├── IngredientResponseDTO.java           # Ingredient details
├── InventoryAdjustmentRequestDTO.java   # Adjust inventory
├── SupplierDTO.java                     # Supplier info
├── PurchaseOrderRequestDTO.java         # Create PO
├── PurchaseOrderResponseDTO.java        # PO details
├── PurchaseOrderDetailRequestDTO.java   # PO item
└── PurchaseOrderDetailResponseDTO.java  # PO item details
```

**Keyword:** `ingredient`, `inventory`, `supplier`, `purchase`

---

#### 💰 **Expense Management**
```
dto/
└── ExpenseDTO.java                      # Expense record
```

**Keyword:** `expense`

---

#### ⏰ **Shift Management** ✅ (Already well-organized!)
```
dto/shift/                               # 24 files grouped
├── ShiftTemplateRequestDTO.java
├── ShiftTemplateResponseDTO.java
├── ShiftInstanceCreateRequestDTO.java
├── ShiftInstanceResponseDTO.java
├── ShiftAssignmentRequestDTO.java
├── ShiftAssignmentResponseDTO.java
├── ShiftSessionStartRequestDTO.java
├── ShiftSessionResponseDTO.java
├── ShiftReportResponseDTO.java
├── AttendanceCheckRequestDTO.java
├── AttendanceRecordResponseDTO.java
├── PayrollCycleRequestDTO.java
├── PayrollSummaryDTO.java
└── ... (11 more)
```

**Keyword:** `shift`, `attendance`, `payroll`, `work`

---

#### 📊 **Analytics & Statistics** ✅ (Partially organized)
```
dto/analytics/                           # ✅ 3 files grouped
├── AdminAnalyticsRequest.java
├── AdminAnalyticsResponse.java
└── DashboardMetricsDTO.java

# At root (related, should be grouped):
├── BestSellerDTO.java                   # ⚠️ Should move
├── HourlySalesDTO.java                  # ⚠️ Should move
├── PaymentMethodStatsDTO.java           # ⚠️ Should move
├── SalesComparisonDTO.java              # ⚠️ Should move
├── StaffPerformanceDTO.java             # ⚠️ Should move
└── CategorySalesDTO.java                # ⚠️ Should move
```

**Keyword:** `analytics`, `sales`, `stats`, `performance`, `best`

---

#### 📈 **Dashboard** ✅ (Partially organized)
```
dto/dashboard/                           # ✅ 3 files grouped
├── AdminDashboardDTO.java
├── ManagerDashboardDTO.java
└── StaffDashboardDTO.java

# At root (related):
└── DashboardStatsDTO.java               # ⚠️ Should move
```

**Keyword:** `dashboard`

---

#### 📝 **Audit Trail** ✅
```
dto/audit/                               # ✅ Grouped
└── AuditLogRequest.java

# Related at root:
└── LoginHistoryResponseDTO.java         # ⚠️ Should move
```

**Keyword:** `audit`, `log`, `history`

---

#### 📎 **File Management**
```
dto/
└── FileUploadResponse.java              # File upload result
```

**Keyword:** `file`, `upload`

---

## 🔍 QUICK SEARCH TABLE

| Need DTO for... | Look in folder | File name pattern |
|-----------------|----------------|-------------------|
| Login/Register | Root | Login*, Register*, Authentication* |
| User CRUD | Root | User* |
| Product CRUD | Root | Product* |
| Category | `category/` | Category* |
| Order CRUD | Root | Order*, OrderDetail* |
| Payment | Root | Payment* |
| Table | Root | CafeTable* |
| Customer | Root | Customer* |
| Voucher | Root | Voucher* |
| Ingredient | Root | Ingredient* |
| Supplier/PO | Root | Supplier*, PurchaseOrder* |
| Expense | Root | Expense* |
| Shift | `shift/` | Shift*, Attendance*, Payroll* |
| Analytics | `analytics/` + Root | *Analytics*, *Sales*, *Stats* |
| Dashboard | `dashboard/` + Root | *Dashboard* |
| Audit | `audit/` + Root | Audit*, LoginHistory* |
| File | Root | File* |

---

## 📦 DTO GROUPING SUMMARY

### ✅ **Well Organized (28 files):**
- `shift/` - 24 files ⭐
- `analytics/` - 3 files
- `dashboard/` - 3 files
- `category/` - 3 files
- `audit/` - 1 file

**Total:** 34 files (56%)

### ⚠️ **Need Organization (26 files):**
- Auth/User DTOs - 7 files
- Product DTOs - 6 files
- Order DTOs - 7 files
- Table DTOs - 3 files
- Customer DTOs - 4 files
- Voucher DTOs - 5 files
- Inventory DTOs - 8 files
- Expense DTOs - 1 file
- File DTOs - 1 file
- Analytics loose - 6 files
- Dashboard loose - 1 file
- Audit loose - 1 file

**Total:** 50 files (44%)

---

## 🎯 FUTURE IDEAL STRUCTURE

```
dto/
├── auth/           # Authentication (7 DTOs)
├── user/           # User management (3 DTOs)
├── product/        # Products (6 DTOs)
├── category/       # Categories (4 DTOs) ✅
├── order/          # Orders (7 DTOs)
├── table/          # Tables (3 DTOs)
├── customer/       # Customers (4 DTOs)
├── voucher/        # Vouchers (5 DTOs)
├── inventory/      # Inventory (8 DTOs)
│   ├── ingredient/
│   └── supplier/
├── expense/        # Expenses (1 DTO)
├── shift/          # Shift system (24 DTOs) ✅
├── analytics/      # Analytics (9 DTOs consolidated)
├── dashboard/      # Dashboard (4 DTOs consolidated)
├── audit/          # Audit trail (2 DTOs)
└── file/           # File management (1 DTO)
```

**Result:** 15 logical groups, easy navigation!

---

## 🛠️ IMPLEMENTATION OPTIONS

### **Option A: Document Only** ⭐ (RECOMMENDED - DONE!)
- ✅ Created package-info.java
- ✅ Created this navigation guide
- ✅ Zero risk, zero migration
- ✅ Easy to find DTOs with docs

### **Option B: Full Restructure**
- Create new folders
- Move 26 files
- Update 160+ import statements
- 2-4 hours work
- Medium risk

### **Option C: Create Symbolic Links**
- Keep files in place
- Create logical grouping via links
- 1 hour work
- Low risk

---

## ✅ RECOMMENDATION

**KEEP CURRENT STRUCTURE + IMPROVE DOCUMENTATION**

**Why:**
1. ✅ Shift module already excellent (24 files grouped)
2. ✅ Some modules already organized
3. ✅ Moving files = update 160+ imports = high risk
4. ✅ Documentation provides same navigation benefit
5. ✅ Can migrate gradually in future

**What We Did:**
- ✅ Created comprehensive DTO package-info.java
- ✅ Created shift/package-info.java
- ✅ Created this navigation guide
- ✅ Documented every DTO's purpose

**Benefit:**
- ✅ Easy to find DTOs (search in docs)
- ✅ Zero risk
- ✅ No breaking changes
- ✅ Production stays stable

---

## 📚 HOW TO USE THIS GUIDE

### Finding a DTO:
1. Open this file (DTO_NAVIGATION_GUIDE.md)
2. Ctrl+F and search keyword (e.g., "product", "order")
3. See exact file location
4. Open file directly

### Understanding DTO purpose:
1. Check package-info.java files
2. Read JavaDoc in DTO file
3. See usage in controllers

### Adding new DTO:
1. Determine domain (product, order, shift, etc.)
2. Check if folder exists (shift/, analytics/, etc.)
3. If yes → put in folder
4. If no → put at root + document in package-info.java

---

**✨ Navigation is now EASY with documentation!**

**Generated:** 2025-11-22  
**Version:** 1.1.0  
**Status:** ✅ Complete

