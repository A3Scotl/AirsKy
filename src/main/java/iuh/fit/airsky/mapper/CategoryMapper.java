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
    Category toEntity(CategoryRequest dto);

    @Mapping(target = "blogCount", ignore = true)
    CategoryResponse toResponseDTO(Category entity);
}
