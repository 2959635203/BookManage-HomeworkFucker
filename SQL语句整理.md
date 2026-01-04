# SQL语句整理文档

本文档整理了项目中所有SQL语句及其用途说明。

## 一、数据库初始化相关SQL

### 1.1 数据库存在性检查
**位置**: `Server/src/main/java/com/northgod/server/config/DatabasePreInitializer.java` (第147行)
```sql
SELECT 1 FROM pg_database WHERE datname = ?
```
**用途**: 检查PostgreSQL数据库中是否存在指定名称的数据库

**位置**: `Server/src/main/java/com/northgod/server/config/DatabaseInitializer.java` (第87行)
```sql
SELECT 1 FROM pg_database WHERE datname = ?
```
**用途**: 检查PostgreSQL数据库中是否存在指定名称的数据库

### 1.2 创建数据库
**位置**: `Server/src/main/java/com/northgod/server/config/DatabasePreInitializer.java` (第167行)
```sql
CREATE DATABASE "bookstore"
```
**用途**: 创建名为bookstore的数据库（数据库名称会被转义以防止SQL注入）

**位置**: `Server/src/main/java/com/northgod/server/config/DatabaseInitializer.java` (第106行)
```sql
CREATE DATABASE "bookstore"
```
**用途**: 创建名为bookstore的数据库（数据库名称会被转义以防止SQL注入）

---

## 二、健康检查相关SQL

### 2.1 数据库连接测试
**位置**: `Server/src/main/java/com/northgod/server/controller/HealthController.java` (第115行、187行)
```sql
SELECT 1
```
**用途**: 简单的数据库连接测试，用于健康检查端点，验证数据库是否可访问

### 2.2 获取数据库版本信息
**位置**: `Server/src/main/java/com/northgod/server/controller/HealthController.java` (第191行)
```sql
SELECT version()
```
**用途**: 获取PostgreSQL数据库的版本信息，用于健康检查报告

### 2.3 查询活跃连接数
**位置**: `Server/src/main/java/com/northgod/server/controller/HealthController.java` (第195行)
```sql
SELECT count(*) FROM pg_stat_activity WHERE application_name LIKE '%Bookstore%'
```
**用途**: 统计当前活跃的数据库连接数，用于监控数据库连接池状态

---

## 三、书籍管理相关SQL (JPQL/Native SQL)

### 3.1 书籍查询

#### 3.1.1 根据ID查询书籍（带悲观锁）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第27行)
```sql
SELECT b FROM Book b WHERE b.id = :id
```
**用途**: 使用悲观锁查询指定ID的书籍，用于更新操作时防止并发冲突

#### 3.1.2 根据ISBN查询活跃书籍
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第30行)
```sql
SELECT b FROM Book b WHERE b.isbn = :isbn AND b.isActive = true
```
**用途**: 根据ISBN号查询未删除的书籍（软删除标记isActive=true）

#### 3.1.3 查询所有活跃书籍（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第37行)
```sql
SELECT b FROM Book b WHERE b.isActive = true
```
**用途**: 分页查询所有未删除的书籍

#### 3.1.4 查询所有已删除书籍（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第40行)
```sql
SELECT b FROM Book b WHERE b.isActive = false
```
**用途**: 分页查询所有已软删除的书籍

#### 3.1.5 查询低库存书籍
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第43行)
```sql
SELECT b FROM Book b WHERE b.isActive = true AND b.stockQuantity <= b.minStock
```
**用途**: 查询库存数量低于最低库存阈值的活跃书籍，用于库存预警

#### 3.1.6 根据库存数量查询书籍（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第46行)
```sql
SELECT b FROM Book b WHERE b.isActive = true AND b.stockQuantity <= :quantity
```
**用途**: 根据指定的库存数量阈值查询书籍，支持分页

#### 3.1.7 关键词搜索书籍（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第49-52行)
```sql
SELECT b FROM Book b WHERE b.isActive = true AND 
    (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
     LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
     b.isbn LIKE CONCAT('%', :keyword, '%'))
```
**用途**: 在标题、作者、ISBN中搜索包含关键词的活跃书籍，支持分页，不区分大小写

#### 3.1.8 快速搜索书籍（原生SQL，性能优化）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第63-69行)
```sql
SELECT b.* FROM book b 
WHERE b.is_active = true 
AND (b.title ILIKE %:keyword% OR b.author ILIKE %:keyword% OR b.isbn LIKE %:keyword%)
ORDER BY b.created_at DESC 
LIMIT 100
```
**用途**: 使用原生SQL快速搜索书籍，最多返回100条结果，按创建时间倒序排列（性能优化版本）

#### 3.1.9 查询最近更新的书籍
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第81-86行)
```sql
SELECT b FROM Book b 
WHERE b.isActive = true 
AND b.updatedAt > :since
ORDER BY b.updatedAt DESC
```
**用途**: 查询指定时间之后更新的活跃书籍，按更新时间倒序排列

### 3.2 书籍统计查询

#### 3.2.1 统计活跃书籍数量
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第72行)
```sql
SELECT COUNT(b) FROM Book b WHERE b.isActive = true
```
**用途**: 统计系统中未删除的书籍总数

#### 3.2.2 统计库存总量
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第75行)
```sql
SELECT SUM(b.stockQuantity) FROM Book b WHERE b.isActive = true
```
**用途**: 计算所有活跃书籍的库存数量总和

#### 3.2.3 按分类统计书籍数量
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第78行)
```sql
SELECT b.category, COUNT(b) as count FROM Book b WHERE b.isActive = true GROUP BY b.category
```
**用途**: 按书籍分类统计每个分类下的书籍数量

#### 3.2.4 计算库存总价值
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第100-106行)
```sql
SELECT COALESCE(SUM(b.purchasePrice * b.stockQuantity), 0) 
FROM Book b 
WHERE b.isActive = true 
AND b.purchasePrice IS NOT NULL 
AND b.stockQuantity > 0
```
**用途**: 计算所有活跃书籍的库存总价值（进价 × 库存数量），用于财务统计

### 3.3 书籍更新操作

#### 3.3.1 更新库存数量
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第56行)
```sql
UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :id
```
**用途**: 增加或减少指定书籍的库存数量（用于进货或销售）

#### 3.3.2 安全更新库存数量（防止负库存）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第60行)
```sql
UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity 
WHERE b.id = :id AND b.stockQuantity + :quantity >= 0
```
**用途**: 更新库存数量，但只有在更新后库存不为负数时才执行，防止负库存

#### 3.3.3 批量软删除书籍
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第94行)
```sql
UPDATE Book b SET b.isActive = false WHERE b.id IN :ids
```
**用途**: 批量将指定ID列表的书籍标记为已删除（软删除），提高性能

---

## 四、交易记录相关SQL (JPQL)

### 4.1 交易查询

#### 4.1.1 根据日期查询交易记录
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第24行)
```sql
SELECT t FROM Transaction t WHERE DATE(t.createdAt) = :date ORDER BY t.createdAt DESC
```
**用途**: 查询指定日期的所有交易记录，按创建时间倒序排列

#### 4.1.2 根据书籍ID查询交易记录（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第27行)
```sql
SELECT t FROM Transaction t WHERE t.book.id = :bookId ORDER BY t.createdAt DESC
```
**用途**: 查询指定书籍的所有交易记录，支持分页，按创建时间倒序

#### 4.1.3 根据年月查询交易记录
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第30行)
```sql
SELECT t FROM Transaction t WHERE YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month ORDER BY t.createdAt DESC
```
**用途**: 查询指定年月的所有交易记录，按创建时间倒序

#### 4.1.4 查询指定年月的销售记录
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第33行)
```sql
SELECT t FROM Transaction t WHERE t.transactionType = 'SALE' AND YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month
```
**用途**: 查询指定年月的所有销售类型交易记录

#### 4.1.5 根据日期范围查询交易记录（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第56行)
```sql
SELECT t FROM Transaction t WHERE t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC
```
**用途**: 查询指定时间范围内的所有交易记录，支持分页，按创建时间倒序

### 4.2 交易统计分析

#### 4.2.1 销售排行榜
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第36-44行)
```sql
SELECT t.book, SUM(t.quantity) as totalQuantity, SUM(t.totalAmount) as totalAmount 
FROM Transaction t 
WHERE t.transactionType = 'SALE' 
AND YEAR(t.createdAt) = :year 
AND MONTH(t.createdAt) = :month 
GROUP BY t.book 
ORDER BY totalQuantity DESC
```
**用途**: 统计指定年月的销售排行榜，按销售数量降序排列，返回书籍、总销售数量和总销售金额

#### 4.2.2 每日销售总额
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第47行)
```sql
SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.transactionType = 'SALE' AND DATE(t.createdAt) = :date
```
**用途**: 计算指定日期的销售总额，如果没有记录则返回0

#### 4.2.3 每日进货总额
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第50行)
```sql
SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.transactionType = 'PURCHASE' AND DATE(t.createdAt) = :date
```
**用途**: 计算指定日期的进货总额，如果没有记录则返回0

#### 4.2.4 统计指定日期的交易数量
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第53行)
```sql
SELECT COUNT(t) FROM Transaction t WHERE DATE(t.createdAt) = :date
```
**用途**: 统计指定日期的交易记录总数

#### 4.2.5 每日交易汇总统计
**位置**: `Server/src/main/java/com/northgod/server/repository/TransactionRepository.java` (第61-71行)
```sql
SELECT 
    DATE(t.createdAt) as transactionDate,
    COUNT(t) as transactionCount,
    SUM(CASE WHEN t.transactionType = 'SALE' THEN t.totalAmount ELSE 0 END) as salesTotal,
    SUM(CASE WHEN t.transactionType = 'PURCHASE' THEN t.totalAmount ELSE 0 END) as purchasesTotal
FROM Transaction t
WHERE t.createdAt >= :startDate AND t.createdAt < :endDate
GROUP BY DATE(t.createdAt)
ORDER BY DATE(t.createdAt) DESC
```
**用途**: 统计指定时间范围内每日的交易汇总信息，包括交易数量、销售总额和进货总额，用于生成报表

---

## 五、供应商管理相关SQL (JPQL)

### 5.1 供应商查询

#### 5.1.1 关键词搜索供应商（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/SupplierRepository.java` (第23-26行)
```sql
SELECT s FROM Supplier s WHERE s.isActive = true AND 
    (LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
     LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
     s.contactPhone LIKE CONCAT('%', :keyword, '%'))
```
**用途**: 在供应商名称、联系人、联系电话中搜索包含关键词的活跃供应商，支持分页，不区分大小写

#### 5.1.2 查询已删除供应商（分页）
**位置**: `Server/src/main/java/com/northgod/server/repository/SupplierRepository.java` (第32行)
```sql
SELECT s FROM Supplier s WHERE s.isActive = false
```
**用途**: 分页查询所有已软删除的供应商

### 5.2 供应商统计

#### 5.2.1 统计活跃供应商数量
**位置**: `Server/src/main/java/com/northgod/server/repository/SupplierRepository.java` (第29行)
```sql
SELECT COUNT(s) FROM Supplier s WHERE s.isActive = true
```
**用途**: 统计系统中未删除的供应商总数

---

## 六、已注释的SQL（历史记录）

### 6.1 查询书籍及其交易记录（已移除）
**位置**: `Server/src/main/java/com/northgod/server/repository/BookRepository.java` (第23行，已注释)
```sql
SELECT b FROM Book b LEFT JOIN FETCH b.transactions WHERE b.id = :id
```
**用途**: 原本用于查询书籍及其关联的所有交易记录（使用左连接预加载），因存在问题已被移除

---

## 七、SQL语句分类统计

### 按操作类型分类：
- **SELECT查询**: 35条
- **UPDATE更新**: 4条
- **CREATE创建**: 2条
- **其他**: 1条（数据库存在性检查）

### 按功能模块分类：
- **数据库初始化**: 4条
- **健康检查**: 3条
- **书籍管理**: 16条
- **交易记录**: 10条
- **供应商管理**: 3条
- **已注释**: 1条

### 按SQL类型分类：
- **JPQL (Java Persistence Query Language)**: 30条
- **Native SQL (原生SQL)**: 4条
- **PostgreSQL系统查询**: 3条

---

## 八、注意事项

1. **软删除机制**: 大部分查询都使用`isActive = true`条件来过滤已删除的记录，实现了软删除功能
2. **性能优化**: 
   - 使用了原生SQL进行快速搜索（`searchBooksFast`方法）
   - 批量操作使用`IN`子句提高效率
   - 使用`COALESCE`函数处理NULL值
3. **并发安全**: 
   - 使用悲观锁（`@Lock(LockModeType.PESSIMISTIC_WRITE)`）防止并发更新冲突
   - 库存更新时检查防止负库存
4. **参数化查询**: 所有SQL都使用参数化查询（`:param`），防止SQL注入攻击
5. **分页支持**: 大部分列表查询都支持分页，提高性能

---

## 九、SQL优化建议

1. **索引建议**: 
   - `book.is_active` 字段应建立索引
   - `book.isbn` 字段应建立唯一索引
   - `transaction.created_at` 字段应建立索引
   - `transaction.transaction_type` 字段应建立索引

2. **查询优化**:
   - 关键词搜索可以考虑使用全文搜索功能
   - 统计查询可以考虑使用物化视图
   - 时间范围查询应确保日期字段有索引

---

**文档生成时间**: 2025-01-XX
**项目名称**: BookManage (图书进销存管理系统)





