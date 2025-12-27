# Mục tiêu hệ thống (Crawl → Lưu cấu trúc → Search → Recommend)

## 1. Mục tiêu & phạm vi
- **Chuỗi xử lý chính**: Crawl dữ liệu → Chuẩn hoá & lưu cấu trúc → Lập chỉ mục (index) → Search → Recommend.
- **Nguyên tắc vận hành**: Toàn bộ pipeline chạy **offline/on-prem**, **không phụ thuộc Python online** (không gọi dịch vụ Python bên ngoài; nếu có Python thì chạy nội bộ theo lịch batch).
- **Đầu ra**:
  - Search API: trả kết quả theo truy vấn/ngữ nghĩa, có bộ lọc.
  - Recommend API: trả danh sách gợi ý theo ngữ cảnh/người dùng/đối tượng.

## 2. Mục tiêu hiệu năng (SLO)
- **Search latency (p95)**: ≤ **150 ms** (tính từ khi nhận query đến khi trả về kết quả).
- **Recommend latency (p95)**: ≤ **250 ms**.
- **Tần suất crawl**:
  - **Incremental**: mỗi **1 giờ**.
  - **Full refresh**: mỗi **7 ngày**.
- **Kích thước corpus mục tiêu**:
  - **10 triệu** bản ghi (docs/items) trong index.
  - Tốc độ tăng trưởng giả định: **~5%/tháng** (để tính quy mô hạ tầng).

## 3. KPI đo lường & chuẩn query plan
- **Crawl latency**: thời gian p95 của job crawl (từ start → hoàn thành ingest).
- **Search latency**: p50/p95/p99 ở tầng API.
- **Index hit ratio**: tỷ lệ truy vấn trả về kết quả từ index (>= 1 hit) / tổng truy vấn.
  - Mục tiêu: **≥ 95%**.
- **Query plan chuẩn (chuẩn hoá pipeline search)**:
  1. **Validate**: kiểm tra tham số, chuẩn hoá text.
  2. **Recall**: truy vấn index (BM25/vector/hybrid).
  3. **Filter**: lọc theo thuộc tính (ACL, category, availability, time-range...).
  4. **Rank**: rerank theo điểm số/feature.
  5. **Diversify**: chống trùng lặp theo nhóm hoặc chủ đề.
  6. **Serve**: trả về payload chuẩn (id, title, score, metadata).

## 4. Checklist vận hành

| Hạng mục | Lịch chạy | Người/nhóm phụ trách | Ghi chú |
| --- | --- | --- | --- |
| Crawl incremental | Mỗi 1 giờ | Data/Platform | Có retry/backoff khi lỗi nguồn |
| Crawl full refresh | Mỗi 7 ngày | Data/Platform | Dọn dữ liệu cũ, chụp snapshot |
| Embed batch | Mỗi 6 giờ | ML/Search | Chỉ chạy nội bộ (offline) |
| Rebuild index | Mỗi 24 giờ | Search | Có rollback index cũ |
| Dedup policy | Hàng ngày | Data | Theo `source_id + normalized_title` |
| Quality report | Hàng tuần | Analytics | Theo dõi hit ratio, latency, coverage |

### Chính sách dedup (tóm tắt)
- **Khoá trùng**: `source_id + normalized_title`.
- **Ưu tiên giữ**: bản ghi mới nhất hoặc có độ đầy đủ metadata cao hơn.
- **Tần suất**: chạy hàng ngày sau crawl incremental.
