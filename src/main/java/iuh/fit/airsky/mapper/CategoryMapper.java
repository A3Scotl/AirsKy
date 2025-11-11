package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.CategoryRequest;
import iuh.fit.airsky.dto.response.CategoryResponse;
import iuh.fit.airsky.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "blogs", ignore = true)
    default Category toEntity(CategoryRequest dto){
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setActive(dto.getActive() != null ? dto.getActive() : true);
        return category;
    }

    default CategoryResponse toResponseDTO(Category entity) {
        if (entity == null) return null;
        CategoryResponse response = new CategoryResponse();
        response.setCategoryId(entity.getCategoryId());
        response.setName(entity.getName());
        response.setSlug(entity.getSlug());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setActive(entity.isActive());
        response.setBlogCount(entity.getBlogs() != null ? (long) entity.getBlogs().size() : 0L);
        return response;
    }
}
