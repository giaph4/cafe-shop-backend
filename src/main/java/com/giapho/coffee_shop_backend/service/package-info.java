/**
 * Business logic layer - Service interfaces and implementations.
 * 
 * <h2>Service Organization:</h2>
 * 
 * <h3>📋 Core Business Services (Root Level):</h3>
 * <ul>
 *   <li><b>AuthenticationService</b> - User authentication & JWT</li>
 *   <li><b>UserService</b> - User management</li>
 *   <li><b>ProductService</b> - Product CRUD operations</li>
 *   <li><b>CategoryService</b> - Category management</li>
 *   <li><b>CustomerService</b> - Customer management</li>
 *   <li><b>IngredientService</b> - Ingredient management</li>
 *   <li><b>SupplierService</b> - Supplier management</li>
 *   <li><b>ExpenseService</b> - Expense tracking</li>
 *   <li><b>FileStorageService</b> - File upload/storage</li>
 *   <li><b>PaymentService</b> - Payment processing</li>
 *   <li><b>AuditLogService</b> - Audit trail</li>
 *   <li><b>LoginHistoryService</b> - Login history tracking</li>
 * </ul>
 * 
 * <h3>📦 Modular Services (Subpackages):</h3>
 * 
 * <h4>order/ - Order Management Module ⭐</h4>
 * <ul>
 *   <li>{@link com.giapho.coffee_shop_backend.service.order.OrderQueryService} - Query operations</li>
 *   <li>{@link com.giapho.coffee_shop_backend.service.order.OrderPricingService} - Pricing calculations</li>
 *   <li>{@link com.giapho.coffee_shop_backend.service.order.OrderValidator} - Order validation</li>
 * </ul>
 * 
 * <h4>shift/ - Shift Management Module ⭐⭐⭐</h4>
 * <p>Comprehensive shift management with 9 services:</p>
 * <ul>
 *   <li>ShiftTemplateService - Shift templates</li>
 *   <li>ShiftInstanceService - Shift instances</li>
 *   <li>ShiftAssignmentService - Staff assignments</li>
 *   <li>ShiftSessionService - Active shift sessions</li>
 *   <li>ShiftReportService - Shift reports</li>
 *   <li>AttendanceService - Attendance tracking</li>
 *   <li>PayrollService - Payroll management</li>
 *   <li>WorkShiftService - Work shift definitions</li>
 *   <li>ShiftPerformanceAdjustmentService - Performance adjustments</li>
 * </ul>
 * 
 * <h4>report/ - Reporting Module ⭐⭐</h4>
 * <p>Advanced reporting with subpackages:</p>
 * <ul>
 *   <li>core/ - Core reporting logic & providers</li>
 *   <li>export/ - Excel export functionality</li>
 *   <li>helper/ - Calculation & validation helpers</li>
 * </ul>
 * 
 * <h4>dashboard/ - Dashboard Module ⭐⭐</h4>
 * <p>Dashboard data aggregation:</p>
 * <ul>
 *   <li>provider/ - Role-specific dashboard providers (Admin, Manager, Staff)</li>
 *   <li>helper/ - User resolution & date helpers</li>
 * </ul>
 * 
 * <h4>Other Modules:</h4>
 * <ul>
 *   <li>purchaseorder/ - Purchase order management</li>
 *   <li>voucher/ - Voucher & discount management</li>
 *   <li>cafetable/ - Table management</li>
 *   <li>supplier/ - Supplier management</li>
 * </ul>
 * 
 * <h3>🔧 Service Implementations:</h3>
 * <p>Most implementations are in {@code impl/} subpackage:</p>
 * <ul>
 *   <li>AuthenticationServiceImpl</li>
 *   <li>UserServiceImpl</li>
 *   <li>ProductServiceImpl</li>
 *   <li>And 24 more...</li>
 * </ul>
 * 
 * <h2>Service Patterns:</h2>
 * 
 * <h3>Constructor Injection (Used Throughout):</h3>
 * <pre>
 * {@code @Service}
 * {@code @RequiredArgsConstructor}
 * public class ProductServiceImpl implements ProductService {
 *     private final ProductRepository repository;
 *     private final ProductMapper mapper;
 *     // Dependencies injected via constructor
 * }
 * </pre>
 * 
 * <h3>Transaction Management:</h3>
 * <pre>
 * {@code @Transactional(readOnly = true)}  // For queries
 * public ProductResponse getById(Long id) { ... }
 * 
 * {@code @Transactional}  // For modifications
 * public ProductResponse create(ProductRequest request) { ... }
 * </pre>
 * 
 * <h3>Caching (New!):</h3>
 * <pre>
 * {@code @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "#id")}
 * public ProductResponse getById(Long id) { ... }
 * 
 * {@code @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true)}
 * public void update(...) { ... }
 * </pre>
 * 
 * <h2>Navigation Tips:</h2>
 * <ul>
 *   <li><b>Find interface:</b> Look in {@code service/} or {@code service/{module}/}</li>
 *   <li><b>Find implementation:</b> Look in {@code service/impl/} or {@code service/{module}/impl/}</li>
 *   <li><b>Find helpers:</b> Look in {@code service/{module}/helper/}</li>
 *   <li><b>Find providers:</b> Look in {@code service/{module}/provider/}</li>
 * </ul>
 * 
 * <h2>Best Practices:</h2>
 * <ul>
 *   <li>✅ Use constructor injection (@RequiredArgsConstructor)</li>
 *   <li>✅ Separate interface and implementation</li>
 *   <li>✅ Use @Transactional appropriately</li>
 *   <li>✅ Validate inputs early</li>
 *   <li>✅ Throw domain-specific exceptions</li>
 *   <li>✅ Log important operations</li>
 *   <li>✅ Keep services focused (Single Responsibility)</li>
 *   <li>✅ Extract complex logic to helper classes</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see com.giapho.coffee_shop_backend.controller Controllers consuming these services
 * @see com.giapho.coffee_shop_backend.domain.repository Repositories used by services
 */
package com.giapho.coffee_shop_backend.service;

