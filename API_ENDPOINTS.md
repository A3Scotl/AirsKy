# API Endpoints Documentation - Blog & Deal System

## Base URL

```
http://localhost:8080/api/v1
```

## Authentication

Most endpoints require JWT token in header:

```
Authorization: Bearer <your-jwt-token>
```

---

## 📝 BLOG APIs

### 1. Get All Blogs (with Pagination)

```http
GET /blogs?page=0&size=10&sort=createdAt,desc
```

**Query Parameters:**

- `page`: Page number (default: 0)
- `size`: Page size (default: 10)
- `sort`: Sort field and direction (default: createdAt,desc)
- `published`: Filter by published status (true/false)
- `categoryId`: Filter by category ID

**Example:**

```http
GET /blogs?page=0&size=5&sort=viewCount,desc&published=true
```

### 2. Get Blog by ID

```http
GET /blogs/{blogId}
```

### 3. Get Blog by Slug

```http
GET /blogs/slug/{slug}
```

### 4. Search Blogs

```http
GET /blogs/search?keyword=travel&page=0&size=10
```

### 5. Get Blogs by Category

```http
GET /blogs/category/{categoryId}?page=0&size=10
```

### 6. Get Blogs by Author

```http
GET /blogs/author/{authorId}?page=0&size=10
```

### 7. Create Blog (Admin only)

```http
POST /blogs
Content-Type: multipart/form-data

title: New Travel Blog
content: <p>Blog content with <strong>HTML</strong> from CKEditor...</p>
excerpt: Short excerpt/summary
featuredImageFile: [IMAGE_FILE] (max 10MB)
isPublished: true
categoryIds: 1,2
```

**Form Fields:**

- `title`: Required, blog title (max 200 characters)
- `content`: Required, HTML content from CKEditor
- `excerpt`: Optional, short summary (max 1000 characters)
- `featuredImageFile`: Optional, image file (max 10MB)
- `featuredImage`: Optional, image URL (if not uploading file)
- `isPublished`: Optional, publish status (default: false)
- `categoryIds`: Required, comma-separated category IDs (e.g., "1,2,3")

**Note:** `slug` will be auto-generated from the `title`. No need to provide it manually.

### 7.1. Upload Image for CKEditor (Admin only)

```http
POST /blogs/upload-image
Content-Type: multipart/form-data

upload: [IMAGE_FILE] (max 10MB)
```

**Response:**

```json
{
  "success": true,
  "message": "Upload ảnh thành công",
  "data": "https://res.cloudinary.com/your-cloud/image/upload/v123456789/airsky/image.jpg"
}
```

### 8. Update Blog (Admin only)

```http
PUT /blogs/{blogId}
Content-Type: multipart/form-data

title: Updated Travel Blog
content: <p>Updated content with <em>HTML</em>...</p>
excerpt: Updated excerpt
featuredImageFile: [NEW_IMAGE_FILE] (max 10MB)
isPublished: true
categoryIds: 1,3
```

**Form Fields:** Same as Create Blog

- `title`: Required, blog title (max 200 characters)
- `content`: Required, HTML content from CKEditor
- `excerpt`: Optional, short summary (max 1000 characters)
- `featuredImageFile`: Optional, new image file (max 10MB)
- `featuredImage`: Optional, image URL (if not uploading new file)
- `isPublished`: Optional, publish status
- `categoryIds`: Required, comma-separated category IDs (e.g., "1,3")
  "categoryIds": [1, 3]
  }

````

**Note:** `slug` will be auto-updated if the `title` changes.

### 9. Delete Blog (Admin only)

```http
DELETE /blogs/{blogId}
````

### 10. Toggle Blog Like (User)

```http
POST /blogs/{blogId}/like
```

### 11. Get Popular Blogs - dựa theo lượt tim/bình luận và lượt xem

```http
GET /blogs/popular?page=0&size=10
```

---

## 💬 BLOG COMMENT APIs

### 1. Get Comments by Blog

```http
GET /blog-comments/blog/{blogId}?page=0&size=10&approved=true
```

### 2. Add Comment (User)

```http
POST /blog-comments
Content-Type: application/json

{
    "blogId": 1,
    "content": "Great article! Very helpful."
}
```

### 3. Get All Comments (Admin)

```http
GET /blog-comments?page=0&size=10&approved=false
```

### 4. Approve Comment (Admin)

```http
PUT /blog-comments/{commentId}/approve
```

### 5. Delete Comment (Admin/Owner)

```http
DELETE /blog-comments/{commentId}
```

---

## 🏷️ CATEGORY APIs

### 1. Get All Categories (with Pagination)

```http
GET /categories?page=0&size=10&sort=name,asc
```

### 2. Get Category by ID

```http
GET /categories/{categoryId}
```

### 3. Create Category (Admin)

```http
POST /categories
Content-Type: application/json

{
    "name": "Adventure Travel",
    "description": "Adventure and extreme travel experiences"
}
```

**Note:** Slug will be automatically generated from the name field (e.g., "Adventure Travel" → "adventure-travel")

### 4. Update Category (Admin)

```http
PUT /categories/{categoryId}
Content-Type: application/json

{
    "name": "Updated Category",
    "description": "Updated description"
}
```

**Note:** If name is changed, slug will be automatically regenerated

### 5. Delete Category (Admin)

```http
DELETE /categories/{categoryId}
```

---

## 🏷️ DEAL APIs

### 1. Get All Deals (with Pagination)

```http
GET /deals?page=0&size=10&sort=createdAt,desc
```

**Query Parameters:**

- `page`: Page number (default: 0)
- `size`: Page size (default: 10)
- `sort`: Sort field and direction
- `active`: Filter by active status (true/false)
- `departureAirportId`: Filter by departure airport
- `arrivalAirportId`: Filter by arrival airport

**Example:**

```http
GET /deals?page=0&size=5&active=true&departureAirportId=1
```

### 2. Get Deal by ID

```http
GET /deals/{dealId}
```

### 3. Get Deal by Code

```http
GET /deals/code/{dealCode}
```

### 4. Validate Deal

```http
POST /deals/{dealCode}/validate
Content-Type: application/json

{
    "bookingAmount": 1500000,
    "departureAirportId": 1,
    "arrivalAirportId": 3
}
```

### 5. Get Active Deals

```http
GET /deals/active?page=0&size=10
```

### 6. Search Deals

```http
GET /deals/search?keyword=summer&page=0&size=10
```

### 7. Get Deals by Route

```http
GET /deals/route?departureAirportId=1&arrivalAirportId=3&page=0&size=10
```

### 8. Create Deal (Admin only)

```http
POST /deals
Content-Type: application/json

{
    "dealCode": "AUTUMN2025",
    "title": "Autumn Special",
    "description": "Special discount for autumn travel",
    "discountPercentage": 12.0,
    "minBookingAmount": 800000,
    "maxDiscountAmount": 300000,
    "usageLimit": 500,
    "isActive": true,
    "validFrom": "2025-09-01T00:00:00",
    "validTo": "2025-11-30T23:59:59",
    "departureAirportId": 1,
    "arrivalAirportId": 2
}
```

### 9. Update Deal (Admin only)

```http
PUT /deals/{dealId}
Content-Type: application/json

{
    "title": "Updated Deal Title",
    "description": "Updated description",
    "discountPercentage": 15.0,
    "isActive": true
}
```

### 10. Delete Deal (Admin only)

```http
DELETE /deals/{dealId}
```

### 11. Get Deal Statistics (Admin only)

```http
GET /deals/{dealId}/statistics
```

### 12. Get Deal Usage History (Admin only)

```http
GET /deals/{dealId}/usage?page=0&size=10
```

### 13. Apply Deal to Booking (User)

```http
POST /deals/{dealCode}/apply
Content-Type: application/json

{
    "bookingId": 123,
    "originalAmount": 1500000
}
```

---

## 📊 Sample API Test Calls

### Test Blog APIs:

```bash
# Get all published blogs with pagination
curl -X GET "http://localhost:8080/api/v1/blogs?page=0&size=5&published=true"

# Get blog by slug
curl -X GET "http://localhost:8080/api/v1/blogs/slug/top-10-travel-destinations-2025"

# Search blogs
curl -X GET "http://localhost:8080/api/v1/blogs/search?keyword=travel&page=0&size=3"
```

### Test Deal APIs:

```bash
# Get all active deals
curl -X GET "http://localhost:8080/api/v1/deals?page=0&size=5&active=true"

# Get deal by code
curl -X GET "http://localhost:8080/api/v1/deals/code/SUMMER2025"

# Validate deal
curl -X POST "http://localhost:8080/api/v1/deals/SUMMER2025/validate" \
  -H "Content-Type: application/json" \
  -d '{"bookingAmount": 1500000, "departureAirportId": 1, "arrivalAirportId": 3}'
```

### Test Category APIs:

```bash
# Get all categories
curl -X GET "http://localhost:8080/api/v1/categories?page=0&size=10"

# Get blogs by category
curl -X GET "http://localhost:8080/api/v1/blogs/category/1?page=0&size=5"
```

---

## 🔒 Role-based Access Control

**PUBLIC (No authentication required):**

- Get blogs, blog details, blog by slug
- Get categories
- Get active deals, deal validation
- Search blogs/deals

**USER (Authentication required):**

- Add blog comments
- Like/unlike blogs
- Apply deals to bookings

**ADMIN (Admin role required):**

- Create/update/delete blogs
- Create/update/delete categories
- Create/update/delete deals
- Manage comments (approve/delete)
- View deal statistics and usage

---

## 📝 Notes

1. **Pagination**: All list endpoints support pagination with `page`, `size`, and `sort` parameters
2. **Filtering**: Most endpoints support filtering by relevant fields
3. **Sorting**: Use format `field,direction` (e.g., `createdAt,desc`, `title,asc`)
4. **Authentication**: Include JWT token in Authorization header for protected endpoints
5. **Error Handling**: All endpoints return appropriate HTTP status codes and error messages
6. **Data Validation**: Request bodies are validated according to the DTO constraints
